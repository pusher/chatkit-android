package com.pusher.chatkit.users

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room


typealias UserSubscriptionConsumer = (UserSubscriptionEvent) -> Unit

sealed class UserSubscriptionEvent {
    internal data class InitialState(
            val rooms: List<Room>,
            val cursors: List<Cursor>,
            val currentUser: User
    ) : UserSubscriptionEvent()
    internal data class AddedToRoomEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RemovedFromRoomEvent(val roomId: String) : UserSubscriptionEvent()
    internal data class RoomUpdatedEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RoomDeletedEvent(val roomId: String) : UserSubscriptionEvent()
    internal data class NewCursor(val cursor: Cursor) : UserSubscriptionEvent()
    internal data class ErrorOccurred(val error: elements.Error) : UserSubscriptionEvent()
}
