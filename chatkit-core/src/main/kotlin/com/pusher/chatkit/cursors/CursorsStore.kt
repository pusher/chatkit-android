package com.pusher.chatkit.cursors

import java.util.concurrent.atomic.AtomicBoolean


class CursorsStore {
    private val map = mutableMapOf<String, UserCursorStore>()
    private val initialized = AtomicBoolean(false)

    operator fun get(userId: String) =
        map[userId] ?: UserCursorStore().also { map[userId] = it }

    operator fun set(userId: String, cursor: Cursor) {
        get(userId) += cursor
    }

    operator fun plusAssign(cursors: List<Cursor>) {
        for (cursor in cursors) {
            this[cursor.userId] += cursor
        }
    }

    fun clear() {
        map.clear()
        initialized.set(false)
    }

    fun applyEvent(event: CursorSubscriptionEvent): List<CursorSubscriptionEvent> =
            when (event) {
                is CursorSubscriptionEvent.InitialState -> {
                    val events = event.cursors.map { cursor ->
                        when (this[cursor.userId][cursor.roomId]) {
                            cursor -> CursorSubscriptionEvent.NoEvent
                            else -> CursorSubscriptionEvent.OnCursorSet(cursor)
                        }
                    }
                    if (initialized.getAndSet(true)) {
                        events
                    } else {
                        listOf()
                    }
                }
                else -> {
                    listOf(event)
                }
            }.also { events ->
                events.forEach { event ->
                    when (event) {
                        is CursorSubscriptionEvent.OnCursorSet -> {
                            this[event.cursor.userId] += event.cursor
                        }
                    }
                }
            }.filterNot {
                it is CursorSubscriptionEvent.NoEvent
            }
}

class UserCursorStore {
    private val cursors = mutableMapOf<String, Cursor>()

    operator fun plusAssign(cursor: Cursor) {
        cursors[cursor.roomId] = cursor
    }

    operator fun get(roomId: String) = cursors[roomId]
}