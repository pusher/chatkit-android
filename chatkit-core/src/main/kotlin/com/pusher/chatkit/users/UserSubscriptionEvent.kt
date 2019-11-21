package com.pusher.chatkit.users

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room


typealias UserSubscriptionConsumer = (UserSubscriptionEvent) -> Unit

sealed class UserSubscriptionEvent {
    internal data class InitialState(
            val rooms: List<Room>,
            val readStates: List<ReadStateApiType>,
            val currentUser: User
    ) : UserSubscriptionEvent() {
        val cursors : List<Cursor>
            get() = readStates.filter { it.cursor != null }
                              .map { it.cursor!! }

    }
    internal data class AddedToRoomEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RemovedFromRoomEvent(val roomId: String) : UserSubscriptionEvent()
    internal data class RoomUpdatedEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RoomDeletedEvent(val roomId: String) : UserSubscriptionEvent()
    internal data class NewCursor(val cursor: Cursor) : UserSubscriptionEvent()
    internal data class ErrorOccurred(val error: elements.Error) : UserSubscriptionEvent()
}
