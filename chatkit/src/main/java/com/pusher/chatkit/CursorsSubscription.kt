package com.pusher.chatkit

import android.os.Handler
import android.os.Looper
import com.pusher.platform.SubscriptionListeners
import elements.Error
import elements.Headers
import elements.SubscriptionEvent

class CursorsSubscription(
        val user: CurrentUser,
        val room: Room,
        val userStore: GlobalUserStore,
        val listeners: CursorsSubscriptionListeners
) {

    val mainThread: Handler = Handler(Looper.getMainLooper())

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
        userStore.fetchUsersWithIds(
                userIds = setOf(cursor.userId),
                onComplete = UsersListener { users ->
                    if (users.isNotEmpty()) {
                        cursor.user = users[0]
                    }
                    mainThread.post { listeners.onCursorSet(cursor) }
                },
                onFailure = ErrorListener {
                    mainThread.post { listeners.onCursorSet(cursor) }
                }
        )
    }

    private fun handleCursorSetInternal(cursor: Cursor) {
        if (cursor.userId == user.id) {
            user.cursors[cursor.roomId] = cursor
        }
    }

    fun handleError(error: Error) {
        mainThread.post { listeners.onError(error) }
    }
}