package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.State
import com.pusher.chatkit.state.UnreadCountReceived
import org.reduxkotlin.reducerForActionType

internal val unreadCountReceivedReducer =
    reducerForActionType<State, UnreadCountReceived> { state, action ->
        checkNotNull(state.joinedRoomsState)

        state.with(
            joinedRoomsState = JoinedRoomsState(
                state.joinedRoomsState.rooms,
                state.joinedRoomsState.unreadCounts.plus(action.roomId to action.unreadCount)
            ),
            auxiliaryState = state.auxiliaryState.with(action)
        )
    }
