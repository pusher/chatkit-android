package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

internal data class ChatState(
    val joinedRoomsState: JoinedRoomsState?
) {
    companion object {
        fun initial() =
            ChatState(
                joinedRoomsState = null
            )
    }

    fun with(joinedRoomsState: JoinedRoomsState) = copy(
        joinedRoomsState = joinedRoomsState
    )
}
