package com.pusher.chatkit.model.mappers

import com.pusher.chatkit.model.network.CreateRoomResponse
import com.pusher.chatkit.model.network.GetRoomResponse
import com.pusher.chatkit.model.network.JoinableRoomsResponse
import com.pusher.chatkit.model.network.JoinedRoomsResponse
import com.pusher.chatkit.model.network.ReadStateApiType
import com.pusher.chatkit.model.network.RoomApiType
import com.pusher.chatkit.model.network.RoomMembershipApiType
import com.pusher.chatkit.rooms.Room

/*
 * FROM INTERNAL REPRESENTATION (managed by room store)
 */
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

/*
 * FROM NETWORK REPRESENTATION (unmanaged get requests)
 */
private fun mapToRoom(
        room: RoomApiType,
        memberships: RoomMembershipApiType?,
        readState: ReadStateApiType?
) = mapToRoom(
        room = room,
        memberships = memberships?.userIds.orEmpty().toSet(),
        unreadCount = readState?.unreadCount
)

internal fun mapToRoom(
        response: GetRoomResponse
) = mapToRoom(
        response.room,
        response.membership,
        null
)

internal fun mapToRoom(
        response: CreateRoomResponse
) = mapToRoom(
        response.room,
        response.membership,
        null
)

internal fun mapToRooms(
        response: JoinableRoomsResponse
) = response.rooms.map { room ->
    mapToRoom(
            room,
            response.memberships.find { it.roomId == room.id },
            null
    )
}

internal fun mapToRooms(
        response: JoinedRoomsResponse
) = response.rooms.map { room ->
    mapToRoom(
            room,
            response.memberships.find { it.roomId == room.id },
            response.readStates.find { it.roomId == room.id }
    )
}