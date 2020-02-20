package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

internal data class ChatkitState(
    val joinedRoomsState: JoinedRoomsState,
    val auxiliaryState: AuxiliaryState
)

internal data class AuxiliaryState(
    val version: Int,
    val joinedRoomsReceived: ReducerLastChange<JoinedRoomsState>
)

internal data class ReducerLastChange<T>(val version: Int, val data: T)