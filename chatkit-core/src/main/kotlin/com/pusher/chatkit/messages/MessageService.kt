package com.pusher.chatkit.messages

import com.pusher.chatkit.*
import com.pusher.chatkit.files.*
import com.pusher.chatkit.util.toJson
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.Error
import java.net.URL
import java.util.concurrent.Future

internal class MessageService(private val chatManager: ChatManager) {

    fun fetchMessages(
        roomId: Int,
        limit: Int,
        initialId: Int?,
        direction: Direction
    ): Future<Result<List<Message>, Error>> =
        fetchMessagesParams(limit, initialId, direction)
            .joinToString(separator = "&", prefix = "?") { (key, value) -> "$key=$value" }
            .let { params ->
                chatManager.doGet<List<Message>>("/rooms/$roomId/messages$params").mapResult { messages ->
                    messages.map { message ->
                        if (message.attachment != null) {
                            val queryParamsMap: Map<String, String> = (URL(message.attachment.link).query?.split("&") ?: emptyList())
                                    .mapNotNull { it.split("=").takeIf { it.size == 2 } }
                                    .map { (key, value) -> key to value }
                                    .toMap()
                            if (queryParamsMap["chatkit_link"] == "true") {
                                message.attachment.fetchRequired = true
                            }
                        }
                        message
                    }
                }
            }

    private fun fetchMessagesParams(
        limit: Int,
        initialId: Int?,
        direction: Direction
    ) = listOfNotNull(
        limit.takeIf { it > 0 }?.let { "limit" to it },
        initialId?.let { "initial_id" to it },
        "direction" to direction
    )

    @JvmOverloads
    fun sendMessage(
        roomId: Int,
        userId: String,
        text: String = "",
        attachment: GenericAttachment = NoAttachment
    ): Future<Result<Int, Error>> =
        attachment.asAttachmentBody(roomId, userId)
            .flatMapFutureResult { sendMessage(roomId, userId, text, it) }

    private fun GenericAttachment.asAttachmentBody(roomId: Int, userId: String): Future<Result<AttachmentBody, Error>> = when (this) {
        is DataAttachment -> chatManager.filesService.uploadFile(this, roomId, userId)
        is LinkAttachment -> AttachmentBody.Resource(link, type.toString())
            .asSuccess<AttachmentBody, elements.Error>()
            .toFuture()
        is NoAttachment -> AttachmentBody.None
            .asSuccess<AttachmentBody, Error>()
            .toFuture()
    }

    private fun sendMessage(
        roomId: Int,
        userId: String,
        text: String = "",
        attachment: AttachmentBody
    ) : Future<Result<Int, Error>> =
        MessageRequest(text, userId, attachment.takeIf { it !== AttachmentBody.None })
            .toJson()
            .toFuture()
            .flatMapFutureResult { body ->
                chatManager.doPost<MessageSendingResponse>("/rooms/$roomId/messages", body)
            }
            .mapResult { it.messageId }

}

private data class MessageSendingResponse(val messageId: Int)

private data class MessageRequest(val text: String? = null, val userId: String, val attachment: AttachmentBody? = null)
