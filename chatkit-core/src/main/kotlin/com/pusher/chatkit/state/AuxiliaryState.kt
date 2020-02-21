package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

internal data class AuxiliaryState(
    val version: Int,
    val joinedRoomsReceived: ReducerLastChange<JoinedRoomsState>?
)

internal data class ReducerLastChange<T>(val version: Int, val data: T)

internal fun applyReducerLastChangeJoinedRoomsState(state: ChatkitState, joinedRoomsState: JoinedRoomsState):
        AuxiliaryState {

    if (state.auxiliaryState != null) {
        val version = state.auxiliaryState.version + 1
        return state.auxiliaryState.copy(version, ReducerLastChange(version, joinedRoomsState))
    } else {
        return AuxiliaryState(0, ReducerLastChange(0, joinedRoomsState))
    }
}
