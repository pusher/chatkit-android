package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.util.DateUtil
import com.pusher.chatkit.util.dateFormat

internal class JoinedRoomApiTypeMapper {
    fun toRoomInternal(room: JoinedRoomApiType): JoinedRoomInternalType {

        var lastMessageAt: Long? = null
        if (room.lastMessageAt != null) {
            lastMessageAt = DateUtil.parseApiDateToEpoch(room.lastMessageAt)
        }

        return JoinedRoomInternalType(
                id = room.id,
                name = room.name,
                isPrivate = room.private,
                createdAt = DateUtil.parseApiDateToEpoch(room.createdAt),
                updatedAt = DateUtil.parseApiDateToEpoch(room.updatedAt),
                customData = room.customData,
                pushNotificationTitleOverride = room.pushNotificationTitleOverride,
                lastMessageAt = lastMessageAt
        )
    }
}
