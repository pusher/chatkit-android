package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.Room

internal class NotJoinedRoomApiMapper {

    internal fun toRooms(response: JoinableRoomsResponse) = response.rooms.map { room ->
        Room(
                id = room.id,
                createdById = room.createdById,
                name = room.name,
                pushNotificationTitleOverride = room.pushNotificationTitleOverride,
                isPrivate = false,
                customData = room.customData,
                unreadCount = null,
                createdAt = room.createdAt,
                lastMessageAt = null,
                updatedAt = room.updatedAt,
                deletedAt = room.deletedAt,
                memberUserIds = emptySet()
        )
    }
}
