package com.pusher.chatkit.users

import com.pusher.chatkit.cursors.Cursor

internal data class ReadStateApiType(
        val room_id: String,
        val unread_count: Int,
        val cursor: Cursor?
)
