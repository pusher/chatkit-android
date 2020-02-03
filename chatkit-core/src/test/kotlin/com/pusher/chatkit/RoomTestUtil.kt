package com.pusher.chatkit

import com.pusher.chatkit.rooms.api.JoinedRoomApiType

internal fun simpleRoom(
        id: String,
        name: String,
        isPrivate: Boolean = false,
        customData: CustomData? = null,
        lastMessageAt: String? = "2017-04-14T14:10:38Z"
) =
        JoinedRoomApiType(
                id = id,
                createdById = "ham",
                name = name,
                pushNotificationTitleOverride = null,
                private = isPrivate,
                customData = customData,
                createdAt = "2017-04-13T14:10:38Z",
                updatedAt = "2017-04-13T14:10:38Z",
                lastMessageAt = lastMessageAt,
                deletedAt = null
        )
