package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.chatStateReducer

internal val roomDeletedReducer =
    chatStateReducer<RoomDeleted> { chatState, action ->
        checkNotNull(chatState.joinedRoomsState)

        chatState.with(
            JoinedRoomsState(
                chatState.joinedRoomsState.rooms - action.roomId,
                chatState.joinedRoomsState.unreadCounts - action.roomId
            )
        )
    }
