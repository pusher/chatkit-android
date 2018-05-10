package com.pusher.chatkit.messages

import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.Error
import java.util.concurrent.Future

internal class MessageService(
    private val chatManager: ChatManager
) {

    private val tokenProvider get() = chatManager.tokenProvider
    private val tokenParams get() = chatManager.dependencies.tokenParams
    private val filesInstance get() = chatManager.filesInstance

    fun fetchMessages(
        roomId: Int,
        limit: Int,
        initialId: Int?,
        direction: Direction
    ): Future<Result<List<Message>, Error>> =
        fetchMessagesParams(limit, initialId, direction)
            .joinToString(separator = "&", prefix = "?") { (key, value) -> "$key=$value" }
            .let { params -> chatManager.doGet("/rooms/$roomId/messages$params") }

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
        text: CharSequence = "",
        attachment: GenericAttachment = NoAttachment
    ): Future<Result<Int, Error>> =
        attachment.asAttachmentBody(roomId)
            .flatMapFutureResult { sendMessage(roomId, userId, text, it) }

    private fun GenericAttachment.asAttachmentBody(roomId: Int): Future<Result<AttachmentBody, Error>> = when (this) {
        is DataAttachment -> uploadFile(this, roomId)
        is LinkAttachment -> AttachmentBody.Resource(link, type.toString())
            .asSuccess<AttachmentBody, elements.Error>()
            .toFuture()
        is NoAttachment -> AttachmentBody.None
            .asSuccess<AttachmentBody, Error>()
            .toFuture()
    }

    private fun uploadFile(
        attachment: DataAttachment,
        roomId: Int
    ): Future<Result<AttachmentBody, Error>> = filesInstance.upload(
        path = "/rooms/$roomId/files/${attachment.name}",
        file = attachment.file,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        responseParser = { it.parseAs<AttachmentBody>() }
    )

    private fun sendMessage(
        roomId: Int,
        userId: String,
        text: CharSequence = "",
        attachment: AttachmentBody
    ) : Future<Result<Int, Error>> =
        MessageRequest(text.toString(), userId, attachment.takeIf { it !== AttachmentBody.None })
            .toJson()
            .toFuture()
            .flatMapFutureResult { body -> chatManager.doPost<MessageSendingResponse>("/rooms/$roomId/messages", body) }
            .mapResult { it.messageId }

}

private data class MessageSendingResponse(val messageId: Int)
