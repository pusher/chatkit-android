package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import org.reduxkotlin.reducerForActionType

internal data class UpdatedRoom(
    val room: JoinedRoomInternalType,
    val unreadCount: Int
)

internal val updatedRoomReducer =
    reducerForActionType<ChatkitState, UpdatedRoom> { state, action ->


        state.with(joinedRoomsState = JoinedRoomsState(
                state.joinedRoomsState!!.rooms
                        .plus(action.room.id to action.room),
                state.joinedRoomsState.unreadCounts
                        .plus(action.room.id to action.unreadCount)
        ))
}
