package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.cursors.Cursor

internal data class RoomReadStateApiType(
    val roomId: String,
    val unreadCount: Int,
    val cursor: Cursor?
)
