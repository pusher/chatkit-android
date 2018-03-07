package com.pusher.chatkit.messages

import com.pusher.chatkit.*
import com.pusher.chatkit.channels.broadcastToChannel
import elements.Subscription
import kotlinx.coroutines.experimental.channels.ReceiveChannel

class MessageService(
    val room: Room,
    private val currentUser: CurrentUser,
    private val chatManager: ChatManager
) {

    private val apiInstance get() = chatManager.apiInstance
    private val tokenProvider get() = chatManager.tokenProvider
    private val tokenParams get() = chatManager.tokenParams
    private val cursorsInstance get() = chatManager.cursorsInstance

    @JvmOverloads
    fun messageEvents(messageLimit: Int? = null, callback: (RoomSubscription.Event) -> Unit): Subscription {
        val roomSubscription = RoomSubscription(room, chatManager.userStore, callback)
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

    @JvmOverloads
    @UsesCoroutines
    fun messageEvents(messageLimit: Int? = null): ReceiveChannel<RoomSubscription.Event> =
        broadcastToChannel { messageEvents(messageLimit) { offer(it) } }

    fun cursorEvents(callback: (CursorsSubscription.Event) -> Unit): Subscription =
        cursorsInstance.subscribeResuming(
            path = "/cursors/0/rooms/${room.id}/",
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            listeners = CursorsSubscription(currentUser, room, chatManager.userStore, callback).subscriptionListeners
        )

    @JvmOverloads
    fun sendMessage(message: String, callback: (MessageSentEvent) -> Unit = {}) {
        currentUser.sendMessage(
            roomId = room.id,
            text = message,
            onCompleteListener = MessageSentListener { id ->
                callback(MessageSentEvent.Successful(id))
            },
            onErrorListener = ErrorListener { error ->
                callback(MessageSentEvent.Failed(error))
            }
        )
    }

}

sealed class MessageSentEvent {
    data class Successful(val messageId: Int) : MessageSentEvent()
    data class Failed(val error: elements.Error) : MessageSentEvent()
}
