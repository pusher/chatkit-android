package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.JoinedRoomsReceived
import com.pusher.chatkit.state.chatStateReducer

internal val joinedRoomsReceivedReducer =
    chatStateReducer<JoinedRoomsReceived> { chatState, action ->
        val joinedRooms = action.rooms.map { it.id to it }.toMap()
        val joinedRoomsState = JoinedRoomsState(joinedRooms, action.unreadCounts)

        chatState.with(joinedRoomsState)
    }
