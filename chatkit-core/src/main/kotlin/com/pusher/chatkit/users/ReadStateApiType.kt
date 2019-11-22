package com.pusher.chatkit.users

import com.pusher.chatkit.cursors.Cursor

internal data class ReadStateApiType(
        val roomId: String,
        val unreadCount: Int,
        val cursor: Cursor?
)
