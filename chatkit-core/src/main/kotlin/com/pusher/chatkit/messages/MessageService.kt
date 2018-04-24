package com.pusher.chatkit.messages

import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.*
import elements.Error

typealias MessagesPromiseResult = Promise<Result<List<Message>, Error>>
typealias MessageIdPromiseResult = Promise<Result<Int, Error>>
typealias AttachmentPromiseResult = Promise<Result<AttachmentBody, Error>>

class MessageService(
    room: Room,
    private val chatManager: ChatManager
) {

    private val roomId = room.id

    private val tokenProvider get() = chatManager.tokenProvider
    private val tokenParams get() = chatManager.dependencies.tokenParams
    private val filesInstance get() = chatManager.filesInstance

    fun fetchMessages(limit: Int = -1): MessagesPromiseResult =
        chatManager.doGet(when {
            limit > 0 -> "/rooms/$roomId/messages?limit=$limit"
            else -> "/rooms/$roomId/messages"
        }).parseResponseWhenReady()

    @JvmOverloads
    fun sendMessage(
        userId: String,
        text: CharSequence = "",
        attachment: GenericAttachment = NoAttachment
    ): MessageIdPromiseResult =
        attachment.asAttachmentBody().flatMapResult { body -> sendMessage(userId, text, body) }

    private fun GenericAttachment.asAttachmentBody(): AttachmentPromiseResult = when (this) {
        is DataAttachment -> uploadFile(this, roomId)
        is LinkAttachment -> AttachmentBody.Resource(link, type).asSuccess<AttachmentBody, elements.Error>().asPromise()
        is NoAttachment -> AttachmentBody.None.asSuccess<AttachmentBody, Error>().asPromise()
    }

    private fun uploadFile(
        attachment: DataAttachment,
        roomId: Int
    ): AttachmentPromiseResult = filesInstance.upload(
        path = "/rooms/$roomId/files/${attachment.name}",
        file = attachment.file,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).parseResponseWhenReady()

    private fun sendMessage(
        userId: String,
        text: CharSequence = "",
        attachment: AttachmentBody
    ): MessageIdPromiseResult =
        MessageRequest(text.toString(), userId, attachment.takeIf { it !== AttachmentBody.None })
            .toJson()
            .map { body -> chatManager.doPost("/rooms/$roomId/messages", body) }
            .map { it.parseResponseWhenReady<MessageSendingResponse>() }
            .recover { error -> error.asFailure<MessageSendingResponse, Error>().asPromise() }
            .mapResult { it.messageId }

}

private data class MessageSendingResponse(val messageId: Int)
