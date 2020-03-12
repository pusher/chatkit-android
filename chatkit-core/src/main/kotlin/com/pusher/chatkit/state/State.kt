package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState
import com.pusher.chatkit.users.state.UserInternalType

@Suppress("CopyWithoutNamedArguments") // auxiliaryState (legible) passes as first param many times
internal data class State(
    val auxiliaryState: AuxiliaryState = AuxiliaryState.initial(),
    val currentUser: UserInternalType? = null,
    val joinedRoomsState: JoinedRoomsState? = null
) {

    companion object {
        fun initial() = State(
            joinedRoomsState = null,
            currentUser = null
        )
    }

    fun with(joinedRoomsState: JoinedRoomsState, auxiliaryState: AuxiliaryState) = copy(
        auxiliaryState = auxiliaryState,
        joinedRoomsState = joinedRoomsState
    )

    fun with(currentUser: UserInternalType, auxiliaryState: AuxiliaryState) = copy(
        auxiliaryState = auxiliaryState,
        currentUser = currentUser
    )
}
