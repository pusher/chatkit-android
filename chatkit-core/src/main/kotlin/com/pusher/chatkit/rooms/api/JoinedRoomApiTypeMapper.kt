package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.util.DateApiTypeMapper

internal class JoinedRoomApiTypeMapper(val dateApiTypeMapper: DateApiTypeMapper) {
    fun toRoomInternalType(room: JoinedRoomApiType): JoinedRoomInternalType {

        var lastMessageAt: Long? = null
        if (room.lastMessageAt != null) {
            lastMessageAt = dateApiTypeMapper.mapToEpochTime(room.lastMessageAt)
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
        val result = arrayListOf<JoinedRoomInternalType>()
        for (room in rooms) {
            result.add(toRoomInternalType(room))
        }
        return result
    }

    fun toUnreadCounts(readStates: List<RoomReadStateApiType>): Map<String, Int> {
        val unreadCounts = hashMapOf<String, Int>()
        for (readState in readStates) {
            unreadCounts[readState.roomId] = readState.unreadCount
        }

        return unreadCounts
    }
}
