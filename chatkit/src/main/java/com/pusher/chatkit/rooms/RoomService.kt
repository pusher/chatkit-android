package com.pusher.chatkit.rooms

import com.pusher.chatkit.CurrentUser
import com.pusher.chatkit.Room

class RoomService(
    private val currentUser: CurrentUser
) {

    fun findBy(id: Int): RoomResult =
        currentUser.rooms()
            .firstOrNull { it.id == id }
            ?.let { RoomResult.Found(it) }
            ?: RoomResult.NotFound

    fun findAll() =
        currentUser.rooms()

}

sealed class RoomResult {
    data class Found(val room: Room) : RoomResult()
    object NotFound: RoomResult()
}
