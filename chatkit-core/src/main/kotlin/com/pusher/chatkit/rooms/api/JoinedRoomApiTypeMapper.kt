package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.util.DateApiTypeMapper

internal class JoinedRoomApiTypeMapper(val dateApiTypeMapper: DateApiTypeMapper) {
    fun toRoomInternalType(room: JoinedRoomApiType): JoinedRoomInternalType {

        val lastMessageAt = room.lastMessageAt?.let {
            dateApiTypeMapper.mapToEpochTime(room.lastMessageAt)
        }

        return JoinedRoomInternalType(
                id = room.id,
                name = room.name,
                isPrivate = room.private,
                createdAt = dateApiTypeMapper.mapToEpochTime(room.createdAt),
                updatedAt = dateApiTypeMapper.mapToEpochTime(room.updatedAt),
                customData = room.customData,
                pushNotificationTitleOverride = room.pushNotificationTitleOverride,
                lastMessageAt = lastMessageAt
        )
    }

    fun toRoomInternalTypes(rooms: List<JoinedRoomApiType>): List<JoinedRoomInternalType> {
        return rooms.map { toRoomInternalType(it) }.toList()
    }

    fun toUnreadCounts(readStates: List<RoomReadStateApiType>): Map<String, Int> {
        return readStates.map { it.roomId to it.unreadCount }.toMap()
    }
}
