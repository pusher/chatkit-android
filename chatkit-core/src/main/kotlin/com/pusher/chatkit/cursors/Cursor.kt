package com.pusher.chatkit.cursors

import com.pusher.chatkit.rooms.HasRoom
import com.pusher.chatkit.users.HasUser
import com.pusher.chatkit.util.dateFormat
import java.util.*

data class Cursor(
    override val userId: String,
    override val roomId: Int,
    val position: Int,
    val updatedAt: String = dateFormat.format(Date()),
    val type: Int = 0
) : HasUser, HasRoom
