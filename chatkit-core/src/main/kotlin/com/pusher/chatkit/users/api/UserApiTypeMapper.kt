package com.pusher.chatkit.users.api

import com.pusher.chatkit.users.state.UserInternalType
import com.pusher.chatkit.util.DateApiTypeMapper

internal class UserApiTypeMapper(private val dateApiTypeMapper: DateApiTypeMapper) {
    fun toUserInternalType(user: UserApiType): UserInternalType =
        UserInternalType(
            id = user.id,
            createdAt = dateApiTypeMapper.mapToEpochTime(user.createdAt),
            updatedAt = dateApiTypeMapper.mapToEpochTime(user.updatedAt),
            name = user.name,
            avatarUrl = user.avatarUrl,
            customData = user.customData
        )
}
