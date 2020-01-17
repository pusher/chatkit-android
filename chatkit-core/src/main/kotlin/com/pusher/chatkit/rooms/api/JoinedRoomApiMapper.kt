package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.Room

internal class JoinedRoomApiMapper {

    fun toRoom(response: CreateRoomResponse) =
            toRoom(
                    response.room,
                    response.membership,
                    unreadCount = 0
            )

    fun toRoom(response: JoinRoomResponse) =
            toRoom(
                    response.room,
                    response.membership,
                    null
            )

    fun toRooms(response: JoinedRoomsResponse) = response.rooms.map { room ->
        toRoom(
                room,
                response.memberships.find { it.roomId == room.id }!!,
                response.readStates.find { it.roomId == room.id }
        )
    }

    private fun toRoom(room: JoinedRoomApiType,
                       membership: RoomMembershipApiType,
                       readState: RoomReadStateApiType?) =
            Room(
                    room.id,
                    room.createdById,
                    room.name,
                    room.pushNotificationTitleOverride,
                    room.private,
                    room.customData,
                    readState?.unreadCount,
                    room.lastMessageAt,
                    room.createdAt,
                    room.updatedAt,
                    room.deletedAt,
                    membership.userIds.toSet()
            )

    private fun toRoom(room: JoinedRoomApiType,
                       membership: RoomMembershipApiType,
                       unreadCount: Int) =
            Room(
                    room.id,
                    room.createdById,
                    room.name,
                    room.pushNotificationTitleOverride,
                    room.private,
                    room.customData,
                    unreadCount,
                    room.lastMessageAt,
                    room.createdAt,
                    room.updatedAt,
                    room.deletedAt,
                    membership.userIds.toSet()
            )

}