package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState
import com.pusher.chatkit.users.state.UserInternalType

internal data class ChatState(
    val joinedRoomsState: JoinedRoomsState?,
    val currentUser: UserInternalType?
) {
    companion object {
        fun initial() =
            ChatState(
                joinedRoomsState = null,
                currentUser = null
            )
    }

    fun with(joinedRoomsState: JoinedRoomsState) = copy(
        joinedRoomsState = joinedRoomsState
    )

    fun with(currentUser: UserInternalType?) = copy(
        currentUser = currentUser
    )
}
