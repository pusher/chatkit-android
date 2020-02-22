@file:Suppress("unused") // TODO: remove when no longer just a sketch (unused)

package com.pusher.chatkit.messages

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.User

data class Message(
    val id: String,
    val sender: User,
    val parts: List<MessagePart>,
    val readByUsers: List<User>,
    val lastReadByUsers: List<User>,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

sealed class MessagePart {
    data class Inline(val mediaType: String, val content: String) : MessagePart()
    data class Link(val mediaType: String, val url: String) : MessagePart()
    data class Attachment(
        val mediaType: String, // TODO: get consensus, it's mimeType in the current swift design
        val id: String,
        val downloadUrl: String,
        val size: Long,

        val expiresAt: Long,
        val refreshUrl: String, // TODO: get consensus about this order

        val name: String?,
        val customData: CustomData?
    ) : MessagePart()
}
