package com.pusher.chatkit.rooms.api

internal data class RoomReadStateApiType(
    val roomId: String,
    val unreadCount: Int
//    val cursor: Cursor?
)
