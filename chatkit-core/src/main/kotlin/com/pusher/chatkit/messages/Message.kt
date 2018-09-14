package com.pusher.chatkit.messages

import com.pusher.chatkit.files.Attachment
import com.pusher.chatkit.rooms.HasRoom
import com.pusher.chatkit.users.User

data class Message(
    val id: Int,
    val userId: String,
    override val roomId: Int,
    val text: String? = null,
    val attachment: Attachment? = null,
    val createdAt: String,
    val updatedAt: String
) : HasRoom {
    var user: User? = null
}
