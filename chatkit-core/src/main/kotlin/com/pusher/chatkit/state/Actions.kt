package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType

internal sealed class Action

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

internal data class ReconnectJoinedRoom(
    val room: JoinedRoomInternalType,
    val unreadCount: Int?
) : Action()

internal data class UnreadCountReceived(
    val roomId: String,
    val unreadCount: Int
) : Action()
