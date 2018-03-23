package com.pusher.chatkit.messages

import com.pusher.annotations.UsesCoroutines
import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.*
import elements.Error
import elements.Subscription

typealias MessagesPromiseResult = Promise<Result<List<Message>, Error>>
typealias MessageIdPromiseResult = Promise<Result<Int, Error>>
typealias AttachmentPromiseResult = Promise<Result<AttachmentBody, Error>>

class MessageService(
    val room: Room,
    private val currentUser: CurrentUser,
    private val chatManager: ChatManager
) {

    private val tokenProvider get() = chatManager.tokenProvider
    private val tokenParams get() = chatManager.tokenParams
    private val filesInstance get() = chatManager.filesInstance

    @JvmOverloads
    fun messageEvents(messageLimit: Int? = null, callback: (Result<Message, Error>) -> Unit): Subscription {
        val roomSubscription = RoomSubscription(room, chatManager.userStore, callback, chatManager)
        with(chatManager) {
            val path = when (messageLimit) {
                null -> "/rooms/${room.id}?user_id=${currentUser.id}"
                else -> "/rooms/${room.id}?user_id=${currentUser.id}&message_limit=$messageLimit"
            }
            return apiInstance.subscribeResuming(
                path = path,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = roomSubscription.subscriptionListeners
            )
        }
    }

    @JvmOverloads
    @UsesCoroutines
    fun messageEvents(messageLimit: Int? = null): Promise<Result<Message, Error>> =
        Promise.promise {
            messageEvents(messageLimit) { report(it) }
        }

    fun cursors(callback: (CursorsSubscription.Event) -> Unit) {
        val cursorsSubscription = CursorsSubscription(currentUser, room, chatManager, callback)
        with(chatManager) {
            cursorsInstance.subscribeResuming(
                path = "/cursors/0/rooms/${room.id}/",
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = cursorsSubscription.subscriptionListeners
            )
        }
    }

    fun fetchMessages(limit: Int = -1): MessagesPromiseResult {
        val path = when {
            limit > 0 -> "/rooms/${room.id}/messages?limit=$limit"
            else -> "/rooms/${room.id}/messages"
        }
        return chatManager.doGet(path)
            .parseResponseWhenReady()
    }

    @JvmOverloads
    fun sendMessage(
        text: String? = null,
        attachment: GenericAttachment = NoAttachment
    ): MessageIdPromiseResult = when (attachment) {
        is DataAttachment -> uploadFile(attachment, room.id)
        is LinkAttachment -> Promise.now(AttachmentBody.Resource(attachment.link, attachment.type).asSuccess<AttachmentBody, elements.Error>())
        is NoAttachment -> Promise.now(AttachmentBody.None.asSuccess<AttachmentBody, Error>())
    }.flatMapResult {
        sendCompleteMessage(text, it)
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

    private fun sendCompleteMessage(
        text: String? = null,
        attachment: AttachmentBody
    ): MessageIdPromiseResult =
        MessageRequest(text = text, userId = currentUser.id, attachment = attachment.takeIf { it !== AttachmentBody.None })
            .toJson()
            .map { body -> chatManager.doPost("/rooms/${room.id}/messages", body) }
            .fold(
                { error -> error.asFailure<MessageSendingResponse, Error>().asPromise() },
                { promise -> promise.parseResponseWhenReady() }
            )
            .mapResult { it.messageId }

}

private data class MessageSendingResponse(val messageId: Int)
