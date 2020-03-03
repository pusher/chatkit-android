package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.LeftRoom
import org.reduxkotlin.reducerForActionType

internal val leftRoomReducer =
    reducerForActionType<ChatkitState, LeftRoom> { state, action ->
        checkNotNull(state.joinedRoomsState)

            state.with(
                    JoinedRoomsState(
                            state.joinedRoomsState.rooms - action.roomId,
                            state.joinedRoomsState.unreadCounts - action.roomId
                    ),
                    state.auxiliaryState.with(action)
            )
}
