package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.DeletedRoom
import org.reduxkotlin.reducerForActionType

internal val roomDeletedReducer =
    reducerForActionType<ChatkitState, DeletedRoom> { state, action ->

        state.with(JoinedRoomsState(
                state.joinedRoomsState!!.rooms - action.roomId,
                state.joinedRoomsState.unreadCounts - action.roomId
        ), state.auxiliaryState.with(action))
}
