package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.chatReducerForActionType

internal val roomUpdatedReducer =
    chatReducerForActionType<RoomUpdated> { chatState, action ->
        checkNotNull(chatState.joinedRoomsState)

        val joinedRoom = action.room.id to action.room

        chatState.with(
            JoinedRoomsState(
                chatState.joinedRoomsState.rooms + joinedRoom,
                chatState.joinedRoomsState.unreadCounts
            )
        )
    }
