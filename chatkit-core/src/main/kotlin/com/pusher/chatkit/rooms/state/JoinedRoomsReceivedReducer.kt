package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.JoinedRoomsReceived
import com.pusher.chatkit.state.chatReducerForActionType

internal val joinedRoomsReceivedReducer =
    chatReducerForActionType<JoinedRoomsReceived> { chatState, action ->
        val joinedRooms = action.rooms.map { it.id to it }.toMap()
        val joinedRoomsState = JoinedRoomsState(joinedRooms, action.unreadCounts)

        chatState.with(joinedRoomsState)
    }
