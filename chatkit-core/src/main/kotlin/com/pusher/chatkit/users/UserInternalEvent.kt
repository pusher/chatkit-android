package com.pusher.chatkit.users

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room

/**
 * Created by the stores when processing [UserSubscriptionEvent]s and used for further processing
 * and translation into public events.
 */
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