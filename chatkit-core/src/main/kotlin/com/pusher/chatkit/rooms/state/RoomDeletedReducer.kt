package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.State
import org.reduxkotlin.reducerForActionType

internal val roomDeletedReducer =
    reducerForActionType<State, RoomDeleted> { state, action ->
        checkNotNull(state.joinedRoomsState)

        state.with(
            JoinedRoomsState(
                state.joinedRoomsState.rooms - action.roomId,
                state.joinedRoomsState.unreadCounts - action.roomId
            ),
            state.auxiliaryState.with(action)
        )
    }
