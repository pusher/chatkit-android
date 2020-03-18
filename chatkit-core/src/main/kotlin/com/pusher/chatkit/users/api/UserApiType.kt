package com.pusher.chatkit.users.api

import com.pusher.chatkit.CustomData

internal data class UserApiType(
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val name: String?,
    val avatarUrl: String?,
    val customData: CustomData?
)
