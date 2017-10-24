package com.pusher.chatkit

import com.pusher.platform.Instance
import java.util.concurrent.ConcurrentMap

class RoomStore(val instance: Instance, val rooms: ConcurrentMap<Int, Room>) {

    fun setOfRooms(): Set<Room>  = rooms.values.toSet()

    fun addOrMerge(room: Room) {
        if (rooms[room.id] != null){
            rooms[room.id]!!.updateWithPropertiesOfRoom(room)
        }
        else{
            rooms.put(room.id, room)
        }
    }
}