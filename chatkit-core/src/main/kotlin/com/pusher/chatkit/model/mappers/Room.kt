package com.pusher.chatkit.model.mappers

import com.pusher.chatkit.model.network.CreateRoomResponse
import com.pusher.chatkit.model.network.GetRoomResponse
import com.pusher.chatkit.model.network.JoinableRoomsResponse
import com.pusher.chatkit.model.network.JoinedRoomsResponse
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.api.RoomApiType
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType

/*
 * FROM INTERNAL REPRESENTATION (managed by room store)
 */
internal fun toRoom(
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

/*
 * FROM NETWORK REPRESENTATION (unmanaged get requests)
 */
private fun toRoom(
        room: RoomApiType,
        memberships: RoomMembershipApiType?,
        readState: RoomReadStateApiType?
) = toRoom(
        room = room,
        memberships = memberships?.userIds.orEmpty().toSet(),
        unreadCount = readState?.unreadCount
)

internal fun toRoom(
        response: GetRoomResponse
) = toRoom(
        response.room,
        response.membership,
        null
)

internal fun toRoom(
        response: CreateRoomResponse
) = toRoom(
        response.room,
        response.membership,
        null
)

internal fun toRooms(
        response: JoinableRoomsResponse
) = response.rooms.map { room ->
    toRoom(
            room,
            response.memberships.find { it.roomId == room.id },
            null
    )
}

internal fun toRooms(
        response: JoinedRoomsResponse
) = response.rooms.map { room ->
    toRoom(
            room,
            response.memberships.find { it.roomId == room.id },
            response.readStates.find { it.roomId == room.id }
    )
}