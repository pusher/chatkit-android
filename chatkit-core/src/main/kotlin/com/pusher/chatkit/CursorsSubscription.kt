package com.pusher.chatkit

import com.pusher.platform.SubscriptionListeners
import elements.*

class CursorsSubscription(
    val user: CurrentUser,
    val room: Room,
    private val chatManager: ChatManager,
    private val onEvent: (Event) -> Unit
) {

    private val subscriptionListeners = SubscriptionListeners(
        onOpen = { },
        onEvent = ::handleCursor,
        onError = ::handleError
    )

    private fun handleCursor(event: SubscriptionEvent<ChatEvent>) {
        val chatEvent = event.body
        when(chatEvent.eventName) {
            "cursor_set" -> chatEvent.cursor
                .also(::handleCursorSetInternal)
                .let { Event.OnCursorSet(it) }
                .let(onEvent)
            else -> subscriptionListeners.onError(Errors.other("Event received is of the wrong type ${chatEvent.eventName}"))
        }
    }

    private val ChatEvent.cursor
        get() = ChatManager.GSON.fromJson<Cursor>(data, Cursor::class.java)

    private fun handleCursorSetInternal(cursor: Cursor) {
        if (cursor.userId == user.id) {
            user.cursors[cursor.roomId] = cursor
        }
        chatManager.userService().fetchUserBy(cursor.userId)
    }

    private fun handleError(error: Error) {
        onEvent(Event.OnError(error))
    }

    sealed class Event {
        data class OnCursorSet(val cursor: Cursor) : Event()
        data class OnError(val error: Error) : Event()
    }

}
