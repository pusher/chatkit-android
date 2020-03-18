package com.pusher.chatkit.users.state

import com.pusher.chatkit.CustomData

internal data class UserInternalType(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val name: String?,
    val avatarUrl: String?,
    val customData: CustomData?
)
