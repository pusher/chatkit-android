package com.pusher.chatkit.users

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room


internal typealias UserSubscriptionConsumer = (UserSubscriptionEvent) -> Unit

internal sealed class UserSubscriptionEvent {

    internal data class InitialState(
            @SerializedName("rooms") private var _rooms: List<Room>,
            val readStates: List<ReadStateApiType>,
            val currentUser: User
    ) : UserSubscriptionEvent() {

        val cursors: List<Cursor>
            get() = readStates.filter { it.cursor != null }
                    .map { it.cursor!! }

        val rooms: List<Room>
            get() {
                if (!populatedRoomUnreadCounts) {
                    _rooms = _rooms.map { room ->
                        Pair(room, readStates.find { readState -> room.id == readState.roomId })
                    }.map { (room, readState) ->
                        if (readState != null) {
                            room.withUnreadCount(readState.unreadCount)
                        } else {
                            room
                        }
                    }
                    populatedRoomUnreadCounts = true
                }
                return _rooms
            }

        private var populatedRoomUnreadCounts = false

    }

    internal data class AddedToRoomEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RemovedFromRoomEvent(val roomId: String) : UserSubscriptionEvent()
    internal data class RoomUpdatedEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RoomDeletedEvent(val roomId: String) : UserSubscriptionEvent()
    internal data class NewCursor(val cursor: Cursor) : UserSubscriptionEvent()
    internal data class ErrorOccurred(val error: elements.Error) : UserSubscriptionEvent()
}
