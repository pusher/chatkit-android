package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import org.reduxkotlin.reducerForActionType

internal data class RoomDeleted(
    val roomId: String
)

internal val roomDeletedReducer =
    reducerForActionType<ChatkitState, RoomDeleted> { state, action ->

        state.with(JoinedRoomsState(
                state.joinedRoomsState!!.rooms - action.roomId,
                state.joinedRoomsState.unreadCounts - action.roomId
        ))
}
