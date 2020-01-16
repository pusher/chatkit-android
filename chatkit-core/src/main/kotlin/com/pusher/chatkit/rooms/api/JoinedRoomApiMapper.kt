package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.Room

internal class JoinedRoomApiMapper {

    fun toRoom(response: GetRoomResponse) =
            toRoom(
                    response.room,
                    response.membership,
                    null
            )

    fun toRoom(response: CreateRoomResponse) =
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

    private fun toRoom(room: RoomApiType,
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

}