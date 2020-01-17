package com.pusher.chatkit.users

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.api.JoinedRoomApiType
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType

internal typealias UserSubscriptionConsumer = (UserSubscriptionEvent) -> Unit

internal sealed class UserSubscriptionEvent {

    internal data class InitialState(
            val currentUser: User,
            val rooms: List<JoinedRoomApiType>,
            val readStates: List<RoomReadStateApiType>,
            val memberships: List<RoomMembershipApiType>
    ) : UserSubscriptionEvent()

    internal data class AddedToRoomEvent(
            var room: JoinedRoomApiType,
            val readState: RoomReadStateApiType,
            val membership: RoomMembershipApiType
    ) : UserSubscriptionEvent()

    internal data class RemovedFromRoomEvent(val roomId: String) : UserSubscriptionEvent()

    internal data class RoomUpdatedEvent(val room: JoinedRoomApiType) : UserSubscriptionEvent()

    internal data class RoomDeletedEvent(val roomId: String) : UserSubscriptionEvent()

    internal data class ReadStateUpdatedEvent(
            val readState: RoomReadStateApiType
    ) : UserSubscriptionEvent()

    internal data class UserJoinedRoomEvent(
            val userId: String,
            val roomId: String
    ) : UserSubscriptionEvent()

    internal data class UserLeftRoomEvent(
            val userId: String,
            val roomId: String
    ) : UserSubscriptionEvent()

    internal data class ErrorOccurred(val error: elements.Error) : UserSubscriptionEvent()
}

internal sealed class UserInternalEvent {
    internal data class AddedToRoom(var room: Room) : UserInternalEvent()
    internal data class RemovedFromRoom(val roomId: String) : UserInternalEvent()
    internal data class RoomUpdated(val room: Room) : UserInternalEvent()
    internal data class RoomDeleted(val roomId: String) : UserInternalEvent()
    internal data class UserJoinedRoom(val userId: String, val roomId: String) : UserInternalEvent()
    internal data class UserLeftRoom(val userId: String, val roomId: String) : UserInternalEvent()
    internal data class NewCursor(val cursor: Cursor) : UserInternalEvent()
    internal data class ErrorOccurred(val error: elements.Error) : UserInternalEvent()
}
