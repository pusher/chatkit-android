package com.pusher.chatkit.messages

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.files.*
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.toJson
import com.pusher.util.Result
import com.pusher.util.asSuccess
import com.pusher.util.collect
import elements.Error
import elements.Errors

internal class MessageService(
        private val client: PlatformClient,
        private val userService: UserService,
        private val filesService: FilesService
) {
    fun fetchMessages(
            roomId: String,
            limit: Int,
            initialId: Int?,
            direction: Direction
    ): Result<List<Message>, Error> =
            fetchMessagesParams(limit, initialId, direction).let { params ->
                client.doGet<List<Message>>("/rooms/$roomId/messages$params").flatMap { messages ->
                    messages.map { message ->
                        userService.fetchUserBy(message.userId).map { user ->
                            message.user = user
                            message
                        }
                    }.collect().mapFailure { errors ->
                        Errors.compose(errors)
                    }
                }
            }

    private fun fetchMessagesParams(limit: Int, initialId: Int?, direction: Direction) =
            listOfNotNull(
                    limit.takeIf { it > 0 }?.let { "limit" to it },
                    initialId?.let { "initial_id" to it },
                    "direction" to direction
            ).joinToString(separator = "&", prefix = "?") { (key, value) ->
                "$key=$value"
            }

    @JvmOverloads
    fun sendMessage(
            roomId: String,
            userId: String,
            text: String = "",
            attachment: GenericAttachment = NoAttachment
    ): Result<Int, Error> =
            attachment.asAttachmentBody(roomId, userId)
                    .flatMap { sendMessage(roomId, userId, text, it) }

    private fun GenericAttachment.asAttachmentBody(
            roomId: String,
            userId: String
    ): Result<AttachmentBody, Error> =
            when (this) {
                is DataAttachment -> filesService.uploadFile(this, roomId, userId)
                is LinkAttachment -> AttachmentBody.Resource(link, type.toString())
                        .asSuccess()
                is NoAttachment -> AttachmentBody.None
                        .asSuccess()
            }

    private fun sendMessage(
            roomId: String,
            userId: String,
            text: String = "",
            attachment: AttachmentBody
    ): Result<Int, Error> =
            MessageRequest(text, userId, attachment.takeIf { it !== AttachmentBody.None })
                    .toJson()
                    .flatMap { body ->
                        client.doPost<MessageSendingResponse>("/rooms/$roomId/messages", body)
                    }.map {
                        it.messageId
                    }
}

private data class MessageSendingResponse(
        val messageId: Int
)

private data class MessageRequest(
        val text: String? = null,
        val userId: String,
        val attachment: AttachmentBody? = null
)
