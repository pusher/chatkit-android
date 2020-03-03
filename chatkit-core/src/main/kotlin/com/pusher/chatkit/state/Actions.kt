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

internal data class DeletedRoom(
    val roomId: String
) : Action()

internal data class UpdatedRoom(
    val room: JoinedRoomInternalType
) : Action()
