package com.pusher.chatkit.messages

import com.google.gson.reflect.TypeToken
import com.pusher.annotations.UsesCoroutines
import com.pusher.chatkit.*
import com.pusher.chatkit.channels.broadcast
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.*
import elements.Error
import elements.Subscription
import kotlinx.coroutines.experimental.channels.ReceiveChannel

typealias MessageListFutureResult = Promise<Result<List<Message>, Error>>
typealias MessageIdResult = Promise<Result<Int, Error>>
typealias AttachmentFutureResult = Promise<Result<AttachmentBody, Error>>

class MessageService(
    val room: Room,
    private val currentUser: CurrentUser,
    private val chatManager: ChatManager
) {

    private val tokenProvider get() = chatManager.tokenProvider
    private val tokenParams get() = chatManager.tokenParams
    private val filesInstance get() = chatManager.filesInstance

    @JvmOverloads
    fun messageEvents(messageLimit: Int? = null, callback: (RoomSubscription.Event) -> Unit): Subscription {
        val roomSubscription = RoomSubscription(room, chatManager.userStore, callback)
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
    fun messageEvents(messageLimit: Int? = null): ReceiveChannel<RoomSubscription.Event> =
        broadcast { messageEvents(messageLimit) { offer(it) } }

    fun cursors(callback: (CursorsSubscription.Event) -> Unit) {
        val cursorsSubscription = CursorsSubscription(currentUser, room, chatManager.userStore, callback)
        with(chatManager) {
            cursorsInstance.subscribeResuming(
                path = "/cursors/0/rooms/${room.id}/",
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = cursorsSubscription.subscriptionListeners
            )
        }
    }

    private val roomListType = object : TypeToken<List<Message>>() {}.type

    fun fetchMessages(): MessageListFutureResult =
        chatManager.doGet("/rooms/${room.id}/messages")
            .parseResponseWhenReady()

    @JvmOverloads
    fun sendMessage(
        text: String? = null,
        attachment: GenericAttachment = NoAttachment
    ): MessageIdResult = when (attachment) {
        is DataAttachment -> uploadFile(attachment, room.id)
        is LinkAttachment -> Promise.now(AttachmentBody.Resource(attachment.link, attachment.type).asSuccess<AttachmentBody, elements.Error>())
        is NoAttachment -> Promise.now(AttachmentBody.None.asSuccess<AttachmentBody, Error>())
    }.flatMapResult {
        sendCompleteMessage(text, it)
    }

    private fun uploadFile(
        attachment: DataAttachment,
        roomId: Int
    ): AttachmentFutureResult = filesInstance.upload(
        path = "/rooms/$roomId/files/${attachment.name}",
        file = attachment.file,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).parseResponseWhenReady()

    private fun sendCompleteMessage(
        text: String? = null,
        attachment: AttachmentBody
    ): MessageIdResult =
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
