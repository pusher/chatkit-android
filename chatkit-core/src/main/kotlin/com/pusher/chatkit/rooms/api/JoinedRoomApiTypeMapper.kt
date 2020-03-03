package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.util.DateUtil

internal class JoinedRoomApiTypeMapper {
    fun toRoomInternalType(room: JoinedRoomApiType): JoinedRoomInternalType {

        var lastMessageAt: Long? = null
        if (room.lastMessageAt != null) {
            lastMessageAt = DateUtil.parseApiDateToEpoch(room.lastMessageAt)
        }

        return JoinedRoomInternalType(
                id = room.id,
                name = room.name,
                isPrivate = room.private,
                createdAt = DateUtil.parseApiDateToEpoch(room.createdAt),
                updatedAt = DateUtil.parseApiDateToEpoch(room.updatedAt),
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
