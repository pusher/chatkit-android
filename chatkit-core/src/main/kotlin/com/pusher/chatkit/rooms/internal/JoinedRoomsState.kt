package com.pusher.chatkit.rooms.internal

internal data class JoinedRoomsState(
    val rooms: Map<String, JoinedRoomInternalType>,
    val unreadCounts: Map<String, Int>
)
