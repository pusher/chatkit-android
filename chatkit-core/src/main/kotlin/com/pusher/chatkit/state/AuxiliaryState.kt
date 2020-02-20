package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

internal data class AuxiliaryState(
    val version: Int,
    val joinedRoomsReceived: ReducerLastChange<JoinedRoomsState>
)

internal data class ReducerLastChange<T>(val version: Int, val data: T)

internal class AuxiliaryStateUtil {
    companion object {

        internal fun applyReducerLastChangeJoinedRoomsState(state: ChatkitState, joinedRoomsState: JoinedRoomsState):
                AuxiliaryState {
            val version = state.auxiliaryState.version + 1
            return state.auxiliaryState.copy(version, ReducerLastChange(version, joinedRoomsState))
        }
    }
}
