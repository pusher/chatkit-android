package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.CustomData

// import com.pusher.chatkit.CustomData

internal data class JoinedRoomApiType(
    val id: String,
    val createdById: String,
    val name: String,
    val pushNotificationTitleOverride: String?,
    val private: Boolean,
    val customData: CustomData?,
    val lastMessageAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
)
