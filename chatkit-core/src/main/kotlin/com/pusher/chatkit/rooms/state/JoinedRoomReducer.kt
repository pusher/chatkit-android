package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.chatStateReducer

internal val joinedRoomReducer =
    chatStateReducer<JoinedRoom> { chatState, action ->
        checkNotNull(chatState.joinedRoomsState)

        val joinedRoom = action.room.id to action.room
        val joinedRoomUnreadCount = action.room.id to action.unreadCount

        chatState.with(
            JoinedRoomsState(
                chatState.joinedRoomsState.rooms + joinedRoom,
                chatState.joinedRoomsState.unreadCounts + joinedRoomUnreadCount
            )
        )
    }
