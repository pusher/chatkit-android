package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

internal data class State(
    val joinedRoomsState: JoinedRoomsState?
) {

    companion object {
        fun initial() = State(
            joinedRoomsState = null
        )
    }

    fun with(joinedRoomsState: JoinedRoomsState) = copy(
        joinedRoomsState = joinedRoomsState
    )
}
