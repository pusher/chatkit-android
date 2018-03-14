package com.pusher.chatkit

import com.pusher.platform.SubscriptionListeners
import com.pusher.util.fold
import elements.Error
import elements.Headers
import elements.SubscriptionEvent

class CursorsSubscription(
    val user: CurrentUser,
    val room: Room,
    private val userStore: GlobalUserStore,
    private val onEvent: (Event) -> Unit
) {

    val subscriptionListeners = SubscriptionListeners(
        onOpen = { handleOpen(it) },
        onEvent = { handleCursor(it) },
        onError = { handleError(it) }
    )

    fun handleOpen(headers: Headers) {
        //TODO("Not handled currently.")
    }

    fun handleCursor(event: SubscriptionEvent) {
        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)
        if (chatEvent.eventName != "cursor_set") {
            TODO("Event received is of the wrong type ${chatEvent.eventName}")
        }
        val cursor = ChatManager.GSON.fromJson<Cursor>(chatEvent.data, Cursor::class.java)
        handleCursorSetInternal(cursor)
        cursor.room = room
        userStore.findOrGetUser(cursor.userId).fold({
            onEvent(Event.OnCursorSet(cursor))
        }, {
            cursor.user = it
            onEvent(Event.OnCursorSet(cursor))
        })
    }

    private fun handleCursorSetInternal(cursor: Cursor) {
        if (cursor.userId == user.id) {
            user.cursors[cursor.roomId] = cursor
        }
    }

    fun handleError(error: Error) {
        onEvent(Event.OnError(error))
    }

    sealed class Event {
        data class OnCursorSet(val cursor: Cursor) : Event()
        data class OnError(val error: Error) : Event()
    }

}