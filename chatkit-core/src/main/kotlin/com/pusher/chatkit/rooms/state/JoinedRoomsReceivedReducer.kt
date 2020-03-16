package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.JoinedRoomsReceived
import com.pusher.chatkit.state.State
import org.reduxkotlin.reducerForActionType

internal val joinedRoomsReceivedReducer =
    reducerForActionType<State, JoinedRoomsReceived> { state, action ->

        val joinedRooms = action.rooms.map { it.id to it }.toMap()
        val joinedRoomsState = JoinedRoomsState(joinedRooms, action.unreadCounts)

        state.with(joinedRoomsState)
    }
