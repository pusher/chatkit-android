package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

@Suppress("CopyWithoutNamedArguments") // auxiliaryState (legible) passes as first param many times
internal data class ChatkitState(
    val auxiliaryState: AuxiliaryState = AuxiliaryState.initial(),
    val joinedRoomsState: JoinedRoomsState?
) {

    companion object {
        fun initial() = ChatkitState(
                joinedRoomsState = null
        )
    }

    fun with(joinedRoomsState: JoinedRoomsState, auxiliaryState: AuxiliaryState) = copy(
        auxiliaryState = auxiliaryState,
        joinedRoomsState = joinedRoomsState
    )
}
