package com.pusher.chatkit

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class RoomStore(private val roomsMap: ConcurrentMap<Int, Room> = ConcurrentHashMap()) {

    val rooms : List<Room>
        get() = roomsMap.values.toList()

    operator fun get(id: Int): Room? =
        roomsMap[id]

    operator fun plusAssign(room: Room) {
        roomsMap += (room.id to room)
    }

    operator fun plusAssign(rooms: List<Room>) {
        roomsMap += rooms.map { it.id to it }.toMap()
    }

    operator fun minusAssign(room: Room) =
        minusAssign(room.id)

    operator fun minusAssign(roomId: Int) {
        roomsMap.remove(roomId)
    }

}
