package com.pusher.chatkit.users

import com.pusher.chatkit.rooms.Room


typealias UserSubscriptionConsumer = (UserSubscriptionEvent) -> Unit

sealed class UserSubscriptionEvent {
    internal data class InitialState(val rooms: List<Room>, val currentUser: User) : UserSubscriptionEvent()
    internal data class AddedToRoomEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RemovedFromRoomEvent(val roomId: Int) : UserSubscriptionEvent()
    internal data class RoomUpdatedEvent(val room: Room) : UserSubscriptionEvent()
    internal data class RoomDeletedEvent(val roomId: Int) : UserSubscriptionEvent()
    internal data class LeftRoomEvent(val roomId: Int, val userId: String) : UserSubscriptionEvent() // v1
    internal data class JoinedRoomEvent(val roomId: Int, val userId: String) : UserSubscriptionEvent() // v1
    internal data class ErrorOccurred(val error: elements.Error) : UserSubscriptionEvent()
}
