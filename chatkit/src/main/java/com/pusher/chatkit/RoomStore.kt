package com.pusher.chatkit

import java.util.concurrent.ConcurrentMap

class RoomStore(val roomsMap: ConcurrentMap<Int, Room>) {

    val rooms : List<Room>
        get() = roomsMap.values.toList()

    fun addOrMerge(room: Room) {
        if (roomsMap[room.id] != null){
            roomsMap[room.id]!!.updateWithPropertiesOfRoom(room)
        } else{
            roomsMap[room.id] = room
        }
    }
}