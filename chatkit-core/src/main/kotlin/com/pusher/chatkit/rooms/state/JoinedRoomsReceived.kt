package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.JoinedRoomsReceived
import org.reduxkotlin.reducerForActionType

internal val joinedRoomsReceivedReducer =
    reducerForActionType<ChatkitState, JoinedRoomsReceived> { state, action ->

        val joinedRooms = action.rooms.map { it.id to it }.toMap()
        val joinedRoomsState = JoinedRoomsState(joinedRooms, action.unreadCounts)
        state.with(joinedRoomsState, state.auxiliaryState.with(action))
}
