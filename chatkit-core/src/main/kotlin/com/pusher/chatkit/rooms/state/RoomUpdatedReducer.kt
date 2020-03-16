package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.State
import org.reduxkotlin.reducerForActionType

internal val roomUpdatedReducer =
    reducerForActionType<State, RoomUpdated> { state, action ->
        checkNotNull(state.joinedRoomsState)

        val joinedRoom = action.room.id to action.room

        state.with(
            JoinedRoomsState(
                state.joinedRoomsState.rooms + joinedRoom,
                state.joinedRoomsState.unreadCounts
            )
        )
    }
