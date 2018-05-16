package com.pusher.chatkit

import com.pusher.chatkit.rooms.Room
import com.pusher.util.Result
import com.pusher.util.mapResult
import elements.Error
import java.util.concurrent.Future

/**
 * Used to inject extension functions to a class that has a [ChatManager]
 */
internal interface HasChat {

    val chatManager: ChatManager

    fun Future<Result<Room, Error>>.saveRoomWhenReady() = mapResult {
        it.also { room ->
            chatManager.roomService.roomStore += room
            chatManager.userService.populateUserStore(room.memberUserIds)
        }
    }

    fun Future<Result<Int, Error>>.removeRoomWhenReady() = mapResult {
        it.also { roomId ->
            chatManager.roomService.roomStore -= roomId
        }
    }

}
