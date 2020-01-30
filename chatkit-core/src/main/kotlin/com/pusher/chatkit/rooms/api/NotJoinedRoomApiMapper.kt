package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.Room

internal class NotJoinedRoomApiMapper {

    internal fun toRooms(response: JoinableRoomsResponse) = response.rooms.map { room ->
        Room(
                id = room.id,
                createdAt = room.createdAt,
                name = room.name,
                pushNotificationTitleOverride = room.pushNotificationTitleOverride,
                isPrivate = false,
                customData = room.customData,
                unreadCount = null,
                lastMessageAt = null,
                createdById = room.createdById,
                updatedAt = room.updatedAt,
                deletedAt = room.deletedAt,
                memberUserIds = emptySet()
        )
    }

}