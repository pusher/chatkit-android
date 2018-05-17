package com.pusher.chatkit.messages

import com.pusher.chatkit.*
import com.pusher.chatkit.files.*
import com.pusher.chatkit.util.toJson
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.Error
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
                chatManager.doGet("/rooms/$roomId/messages$params")
            }

    private fun fetchMessagesParams(
        limit: Int,
        initialId: Int?,
        direction: Direction
    ) = listOfNotNull(
        limit.takeIf { it > 0 }?.let { "limit" to it },
        initialId?.let { "initialId" to it },
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
