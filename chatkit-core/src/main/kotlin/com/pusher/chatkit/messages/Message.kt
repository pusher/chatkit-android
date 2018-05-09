package com.pusher.chatkit.messages

import com.pusher.chatkit.rooms.HasRoom
import com.pusher.chatkit.users.HasUser

data class Message(
    val id: Int,
    override val userId: String,
    override val roomId: Int,
    val text: String? = null,
    val attachment: Attachment? = null,
    val createdAt: String,
    val updatedAt: String
) : HasRoom, HasUser
