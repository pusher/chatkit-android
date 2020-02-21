package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

internal data class ChatkitState(
    val joinedRoomsState: JoinedRoomsState?,
    val auxiliaryState: AuxiliaryState?
)
