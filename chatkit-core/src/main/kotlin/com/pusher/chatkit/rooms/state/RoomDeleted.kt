package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.applyReducerLastChangeJoinedRoomsState
import org.reduxkotlin.reducerForActionType

internal data class RoomDeleted(
    val roomId: String
)

internal val roomDeletedReducer =
    reducerForActionType<ChatkitState, RoomDeleted> { state, action ->

            var joinedRoomsState = JoinedRoomsState(
                    hashMapOf(),
                    hashMapOf()
            )

            if (state.joinedRoomsState != null) {
                joinedRoomsState = JoinedRoomsState(
                        state.joinedRoomsState.rooms.filterNot { it.key == action.roomId },
                        state.joinedRoomsState.unreadCounts.filterNot { it.key == action.roomId }
                )
            }

        state.copy(
            joinedRoomsState,
            applyReducerLastChangeJoinedRoomsState(state, joinedRoomsState)
        )
}
