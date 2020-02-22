package com.pusher.chatkit.rooms

// JoinedRoom?
data class Room(
    val id: String,
    val name: String,
    val isPrivate: Boolean,
    val createdById: String,
    val unreadCount: Int?,
    val lastMessageAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
