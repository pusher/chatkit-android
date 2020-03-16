package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.chatReducerForActionType

internal val leftRoomReducer =
    chatReducerForActionType<LeftRoom> { chatState, action ->
        checkNotNull(chatState.joinedRoomsState)

        chatState.with(
            JoinedRoomsState(
                chatState.joinedRoomsState.rooms - action.roomId,
                chatState.joinedRoomsState.unreadCounts - action.roomId
            )
        )
    }
