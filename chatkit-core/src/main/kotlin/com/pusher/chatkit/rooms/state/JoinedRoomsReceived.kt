package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import org.reduxkotlin.reducerForActionType

internal data class JoinedRoomsReceived(
    val rooms: List<JoinedRoomInternalType>,
    val unreadCounts: Map<String, Int>
)

internal val joinedRoomsReceivedReducer =
    reducerForActionType<ChatkitState, JoinedRoomsReceived> { state, action ->

        val joinedRooms = action.rooms.map {
            Pair(it.id, it)
        }.toMap()

        val joinedRoomsState = JoinedRoomsState(joinedRooms, action.unreadCounts)

        state.with(joinedRoomsState)
}
