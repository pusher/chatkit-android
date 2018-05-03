package com.pusher.chatkit.messages

import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.Error
import java.util.concurrent.Future

class MessageService(
    room: Room,
    private val chatManager: ChatManager
) {

    private val roomId = room.id

    private val tokenProvider get() = chatManager.tokenProvider
    private val tokenParams get() = chatManager.dependencies.tokenParams
    private val filesInstance get() = chatManager.filesInstance

    fun fetchMessages(limit: Int = -1): Future<Result<List<Message>, Error>> =
        chatManager.doGet(when {
            limit > 0 -> "/rooms/$roomId/messages?limit=$limit"
            else -> "/rooms/$roomId/messages"
        })

    @JvmOverloads
    fun sendMessage(
        userId: String,
        text: CharSequence = "",
        attachment: GenericAttachment = NoAttachment
    ): Future<Result<Int, Error>> =
        attachment.asAttachmentBody()
            .flatMapFutureResult { sendMessage(userId, text, it) }


    private fun GenericAttachment.asAttachmentBody(): Future<Result<AttachmentBody, Error>> = when (this) {
        is DataAttachment -> uploadFile(this, roomId)
        is LinkAttachment -> Futures.now(AttachmentBody.Resource(link, type).asSuccess<AttachmentBody, elements.Error>())
        is NoAttachment -> Futures.now(AttachmentBody.None.asSuccess<AttachmentBody, Error>())
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
