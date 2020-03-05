package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.users.state.UserInternalType

internal sealed class Action

internal data class CurrentUserReceived(
    val currentUser: UserInternalType
) : Action()

internal data class JoinedRoomsReceived(
    val rooms: List<JoinedRoomInternalType>,
    val unreadCounts: Map<String, Int>
) : Action()

internal data class JoinedRoom(
    val room: JoinedRoomInternalType,
    val unreadCount: Int
) : Action()

internal data class LeftRoom(
    val roomId: String
) : Action()

internal data class RoomDeleted(
    val roomId: String
) : Action()

internal data class RoomUpdated(
    val room: JoinedRoomInternalType
) : Action()
