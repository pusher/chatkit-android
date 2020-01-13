package com.pusher.chatkit.model.mappers

import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.RoomApiType
import com.pusher.chatkit.users.RoomMembershipApiType

internal fun mapToRoom(
        room: RoomApiType,
        memberships: RoomMembershipApiType?,
        readState: ReadStateApiType?
) = mapToRoom(
        room = room,
        memberships = memberships?.userIds.orEmpty().toSet(),
        unreadCount = readState?.unreadCount
)

internal fun mapToRoom(
        room: RoomApiType,
        memberships: Set<String>?,
        unreadCount: Int?
) = Room(
        id = room.id,
        name = room.name,
        createdById = room.createdById,
        pushNotificationTitleOverride = room.pushNotificationTitleOverride,
        isPrivate = room.private,
        customData = room.customData,
        createdAt = room.createdAt,
        updatedAt = room.updatedAt,
        deletedAt = room.deletedAt,
        lastMessageAt = room.lastMessageAt,
        unreadCount = unreadCount,
        memberUserIds = memberships.orEmpty()
)