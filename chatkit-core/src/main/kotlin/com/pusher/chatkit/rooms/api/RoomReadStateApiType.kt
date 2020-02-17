package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.cursors.api.CursorApiType

internal data class RoomReadStateApiType(
    val roomId: String,
    val unreadCount: Int,
    val cursor: CursorApiType?
)
