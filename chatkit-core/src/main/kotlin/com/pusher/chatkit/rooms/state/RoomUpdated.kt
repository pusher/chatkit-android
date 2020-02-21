package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.applyReducerLastChangeJoinedRoomsState
import org.reduxkotlin.reducerForActionType

internal data class RoomUpdated(
    val room: JoinedRoomInternalType,
    val unreadCount: Int
)

internal val roomUpdatedReducer =
    reducerForActionType<ChatkitState, RoomUpdated> { state, action ->

            var joinedRoomsState = JoinedRoomsState(
                    hashMapOf(
                            Pair(action.room.id, action.room)),
                    hashMapOf(
                            Pair(action.room.id, action.unreadCount))
            )

            if (state.joinedRoomsState != null) {
                    joinedRoomsState = JoinedRoomsState(
                            state.joinedRoomsState.rooms
                                    .filterNot { it.key == action.room.id }
                                    .plus(Pair(action.room.id, action.room)),
                            state.joinedRoomsState.unreadCounts
                                    .filterNot { it.key == action.room.id }
                                    .plus(Pair(action.room.id, action.unreadCount))
                    )
            }

        state.copy(
            joinedRoomsState,
            applyReducerLastChangeJoinedRoomsState(state, joinedRoomsState)
        )
}
