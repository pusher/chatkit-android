package com.pusher.chatkit.messages

import com.pusher.chatkit.*
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel

class MessageService(
    val room: Room,
    private val currentUser: CurrentUser,
    private val chatManager: ChatManager
) {

    @JvmOverloads
    fun messageEvents(messageLimit: Int? = null, callback: (RoomSubscription.Event) -> Unit) {
        val roomSubscription = RoomSubscription(room, chatManager.userStore, callback)
        with(chatManager) {
            val path = when (messageLimit) {
                null -> "/rooms/${room.id}?user_id=${currentUser.id}"
                else -> "/rooms/${room.id}?user_id=${currentUser.id}&message_limit=$messageLimit"
            }
            apiInstance.subscribeResuming(
                path = path,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = roomSubscription.subscriptionListeners
            )
        }
    }

    @JvmOverloads
    @UsesCoroutines
    fun messageEvents(messageLimit: Int? = null): SubscriptionReceiveChannel<RoomSubscription.Event> =
        with(BroadcastChannel<RoomSubscription.Event>(Channel.CONFLATED)) {
            messageEvents(messageLimit) { offer(it) }
            openSubscription()
        }

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