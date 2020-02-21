package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.CustomData

internal data class JoinedRoomInternalType(
    val id: String,
    val name: String,
    val isPrivate: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessageAt: Long?,
    val customData: CustomData?,
    val pushNotificationTitleOverride: String?
)
