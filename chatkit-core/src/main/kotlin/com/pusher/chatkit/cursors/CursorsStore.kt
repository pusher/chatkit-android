package com.pusher.chatkit.cursors


class CursorsStore {
    private val map = mutableMapOf<String, UserCursorStore>()

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

    fun applyEvent(event: CursorSubscriptionEvent): List<CursorSubscriptionEvent> =
            when (event) {
                is CursorSubscriptionEvent.InitialState -> {
                    event.cursors.map { cursor ->
                        when (this[cursor.userId][cursor.roomId]) {
                            cursor -> CursorSubscriptionEvent.NoEvent
                            else -> CursorSubscriptionEvent.OnCursorSet(cursor)
                        }
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