package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.State
import org.reduxkotlin.reducerForActionType

internal val joinedRoomReducer =
    reducerForActionType<State, JoinedRoom> { state, action ->
        val chatState = state.chatState
        checkNotNull(chatState.joinedRoomsState)

        val joinedRoom = action.room.id to action.room
        val joinedRoomUnreadCount = action.room.id to action.unreadCount

        state.with(
            chatState.with(
                JoinedRoomsState(
                    chatState.joinedRoomsState.rooms + joinedRoom,
                    chatState.joinedRoomsState.unreadCounts + joinedRoomUnreadCount
                )
            )
        )
    }
