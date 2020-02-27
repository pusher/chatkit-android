package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import org.reduxkotlin.reducerForActionType

internal data class LeftRoom(
    val roomId: String
)

internal val leftRoomReducer =
    reducerForActionType<ChatkitState, LeftRoom> { state, action ->
        state.with(JoinedRoomsState(
                state.joinedRoomsState!!.rooms - action.roomId,
                state.joinedRoomsState.unreadCounts - action.roomId
        ))
}
