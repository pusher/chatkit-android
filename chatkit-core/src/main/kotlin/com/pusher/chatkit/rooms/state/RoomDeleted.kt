package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import org.reduxkotlin.reducerForActionType

internal data class RoomDeleted(
    val roomId: String
)

internal val roomDeletedReducer =
    reducerForActionType<ChatkitState, RoomDeleted> { state, action ->

            var joinedRoomsState = JoinedRoomsState(
                    mapOf(),
                    mapOf()
            )

            if (state.joinedRoomsState != null) {
                joinedRoomsState = JoinedRoomsState(
                        state.joinedRoomsState.rooms.filterNot { it.key == action.roomId },
                        state.joinedRoomsState.unreadCounts.filterNot { it.key == action.roomId }
                )
            }

        state.with(joinedRoomsState)
}
