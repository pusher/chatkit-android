package com.pusher.chatkit.users.api

import com.pusher.chatkit.users.state.UserInternalType
import com.pusher.chatkit.util.DateApiTypeMapper

internal class UserApiTyepMapper(val dateApiTypeMapper: DateApiTypeMapper) {
    fun toUserInternalType(user: UserApiType): UserInternalType {
        return UserInternalType(
            id = user.id,
            createdAt = dateApiTypeMapper.mapToEpochTime(user.createdAt),
            name = user.name,
            avatarUrl = user.avatarUrl,
            updatedAt = dateApiTypeMapper.mapToEpochTime(user.updatedAt),
            customData = user.customData,
            online = user.online
        )
    }
}
