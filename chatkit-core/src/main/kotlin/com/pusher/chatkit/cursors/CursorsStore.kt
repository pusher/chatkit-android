package com.pusher.chatkit.cursors

internal class CursorsStore {
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

}

internal class UserCursorStore {
    private val cursors = mutableMapOf<Int, Cursor>()

    operator fun plusAssign(cursor: Cursor) {
        cursors[cursor.roomId] = cursor
    }

    operator fun get(roomId: Int) = cursors[roomId]

}