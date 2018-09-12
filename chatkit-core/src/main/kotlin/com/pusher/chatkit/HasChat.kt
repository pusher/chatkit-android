package com.pusher.chatkit

import com.pusher.chatkit.rooms.Room
import com.pusher.util.Result
import elements.Error

/**
 * Used to inject extension functions to a class that has a [ChatManager]
 */
internal interface HasChat {

    val chatManager: ChatManager

    fun Result<Room, Error>.saveRoomWhenReady() = map {
        it.also { room ->
            chatManager.roomService.roomStore += room
            chatManager.userService.populateUserStore(room.memberUserIds)
        }
    }

    fun Result<Int, Error>.removeRoomWhenReady() = map {
        it.also { roomId ->
            chatManager.roomService.roomStore -= roomId
        }
    }

}
