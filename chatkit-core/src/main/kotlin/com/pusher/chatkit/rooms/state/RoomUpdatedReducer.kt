package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.RoomUpdated
import org.reduxkotlin.reducerForActionType

internal val roomUpdatedReducer =
    reducerForActionType<ChatkitState, RoomUpdated> { state, action ->
        checkNotNull(state.joinedRoomsState)

        state.with(joinedRoomsState = JoinedRoomsState(
                state.joinedRoomsState.rooms
                        .plus(action.room.id to action.room),
                state.joinedRoomsState.unreadCounts
        ), auxiliaryState = state.auxiliaryState.with(action))
}
