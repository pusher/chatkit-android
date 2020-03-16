package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.State
import org.reduxkotlin.reducerForActionType

internal val roomDeletedReducer =
    reducerForActionType<State, RoomDeleted> { state, action ->
        val chatState = state.chatState
        checkNotNull(chatState.joinedRoomsState)

        state.with(
            chatState.with(
                JoinedRoomsState(
                    chatState.joinedRoomsState.rooms - action.roomId,
                    chatState.joinedRoomsState.unreadCounts - action.roomId
                )
            )
        )
    }
