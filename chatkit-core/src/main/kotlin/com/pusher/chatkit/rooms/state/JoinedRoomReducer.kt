package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.State
import com.pusher.chatkit.state.JoinedRoom
import org.reduxkotlin.reducerForActionType

internal val joinedRoomReducer =
    reducerForActionType<State, JoinedRoom> { state, action ->
        checkNotNull(state.joinedRoomsState)

        val joinedRoom = action.room.id to action.room
        val joinedRoomUnreadCount = action.room.id to action.unreadCount

        state.with(
            JoinedRoomsState(
                state.joinedRoomsState.rooms + joinedRoom,
                state.joinedRoomsState.unreadCounts + joinedRoomUnreadCount
            ),
            state.auxiliaryState.with(action)
        )
    }
