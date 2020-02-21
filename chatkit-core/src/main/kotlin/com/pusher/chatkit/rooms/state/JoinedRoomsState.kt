package com.pusher.chatkit.rooms.state

internal data class JoinedRoomsState(
    val rooms: Map<String, JoinedRoomInternalType>,
    val unreadCounts: Map<String, Int>
)
