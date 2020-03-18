package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState
import com.pusher.chatkit.users.state.UserInternalType

internal data class ChatState(
    val currentUser: UserInternalType?,
    val joinedRoomsState: JoinedRoomsState?
) {
    companion object {
        fun initial() =
            ChatState(
                currentUser = null,
                joinedRoomsState = null
            )
    }

    fun with(joinedRoomsState: JoinedRoomsState) = copy(
        joinedRoomsState = joinedRoomsState
    )

    fun with(currentUser: UserInternalType?) = copy(
        currentUser = currentUser
    )
}
