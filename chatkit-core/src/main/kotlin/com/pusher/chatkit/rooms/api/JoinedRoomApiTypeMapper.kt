package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.util.dateFormat

internal class JoinedRoomApiTypeMapper {
    fun toRoomInternal(room: JoinedRoomApiType): JoinedRoomInternalType {

        var lastMessageAt : Long? = null
        if (room.lastMessageAt != null) {
            lastMessageAt = dateFormat.parse(room.lastMessageAt).time
        }

        return JoinedRoomInternalType(
                id = room.id,
                name = room.name,
                isPrivate = room.private,
                createdAt = dateFormat.parse(room.createdAt).time,
                updatedAt = dateFormat.parse(room.updatedAt).time,
                customData = room.customData,
                pushNotificationTitleOverride = room.pushNotificationTitleOverride,
                lastMessageAt = lastMessageAt
        )
    }
}