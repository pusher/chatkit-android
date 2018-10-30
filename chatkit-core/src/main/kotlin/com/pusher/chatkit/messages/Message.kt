package com.pusher.chatkit.messages

import com.pusher.chatkit.files.Attachment
import com.pusher.chatkit.users.User

data class Message(
    val id: Int,
    val userId: String,
    val roomId: String,
    val text: String? = null,
    val attachment: Attachment? = null,
    val createdAt: String,
    val updatedAt: String
) {
    var user: User? = null
}
