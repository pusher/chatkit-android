package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.CustomData

// import com.pusher.chatkit.CustomData

internal data class NotJoinedRoomApiType(
    val id: String,
    val createdById: String,
    val name: String,
    val pushNotificationTitleOverride: String?,
    val customData: CustomData?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
)
