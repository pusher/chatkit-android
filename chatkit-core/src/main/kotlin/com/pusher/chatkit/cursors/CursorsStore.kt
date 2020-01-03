package com.pusher.chatkit.cursors

import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.UserSubscriptionEvent


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

    fun clear() {
        map.clear()
    }

    fun initialiseContents(cursors: List<Cursor>) {
        clear()
        this += cursors
    }

    internal fun applyEvent(event: UserSubscriptionEvent): List<UserSubscriptionEvent> =
            when (event) {
                is UserSubscriptionEvent.InitialState ->
                    integrateCursors(event.cursors)
                            .map { cursor -> findCorrespondingReadState(event, cursor) }
                            .map(UserSubscriptionEvent::ReadStateUpdatedEvent)
                is UserSubscriptionEvent.ReadStateUpdatedEvent ->
                    applyReadState(event.readState)
                is UserSubscriptionEvent.AddedToRoomApiEvent ->
                    applyReadState(event.readState)
                else ->
                    listOf()
            }

    private fun applyReadState(readState: ReadStateApiType) : List<UserSubscriptionEvent> =
            if (readState.cursor != null) {
                integrateCursors(listOf(readState.cursor))
                        .map { UserSubscriptionEvent.ReadStateUpdatedEvent(readState) }
            } else {
                listOf()
            }

    private fun findCorrespondingReadState(event: UserSubscriptionEvent.InitialState,
                                           cursor: Cursor) =
            event.readStates.find { it.cursor?.matches(cursor) ?: false }!!

    fun applyEvent(event: CursorSubscriptionEvent): List<CursorSubscriptionEvent> =
            when (event) {
                is CursorSubscriptionEvent.InitialState ->
                    integrateCursors(event.cursors).map(CursorSubscriptionEvent::OnCursorSet)
                is CursorSubscriptionEvent.OnCursorSet ->
                    integrateCursors(listOf(event.cursor)).map(CursorSubscriptionEvent::OnCursorSet)
                else ->
                    listOf(event)
            }.filterNot {
                it is CursorSubscriptionEvent.NoEvent
            }

    private fun integrateCursors(newState: List<Cursor>): List<Cursor> =
            newState.mapNotNull { cursor ->
                if (this[cursor.userId][cursor.roomId]?.position == cursor.position) {
                    null
                } else {
                    this[cursor.userId] += cursor
                    cursor
                }
            }
}

class UserCursorStore {
    private val cursors = mutableMapOf<String, Cursor>()

    operator fun plusAssign(cursor: Cursor) {
        cursors[cursor.roomId] = cursor
    }

    operator fun get(roomId: String) = cursors[roomId]
}