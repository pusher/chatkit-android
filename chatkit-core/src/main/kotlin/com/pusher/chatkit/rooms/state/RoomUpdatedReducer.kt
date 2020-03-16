package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.State
import org.reduxkotlin.reducerForActionType

internal val roomUpdatedReducer =
    reducerForActionType<State, RoomUpdated> { state, action ->
        val chatState = state.chatState
        checkNotNull(chatState.joinedRoomsState)

        val joinedRoom = action.room.id to action.room

        state.with(
            chatState.with(
                JoinedRoomsState(
                    chatState.joinedRoomsState.rooms + joinedRoom,
                    chatState.joinedRoomsState.unreadCounts
                )
            )
        )
    }
