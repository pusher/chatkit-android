package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.ChatkitState
import org.reduxkotlin.reducerForActionType

internal data class JoinedRoom(
    val room: JoinedRoomInternalType,
    val unreadCount: Int
)

internal val joinedRoomReducer =
    reducerForActionType<ChatkitState, JoinedRoom> { state, action ->

            val joinedRoom = Pair(action.room.id, action.room)
            val joinedRoomUnreadCount = Pair(action.room.id, action.unreadCount)
            var joinedRoomsState = JoinedRoomsState(
                    mapOf(joinedRoom),
                    mapOf(joinedRoomUnreadCount)
            )

            if (state.joinedRoomsState != null) {
                    val totalJoinedRooms = (state.joinedRoomsState.rooms +
                            joinedRoom)
                    val totalUnreadRooms = (state.joinedRoomsState.unreadCounts +
                            joinedRoomUnreadCount)
                    joinedRoomsState = JoinedRoomsState(
                            totalJoinedRooms,
                            totalUnreadRooms
                    )
            }

        state.with(joinedRoomsState)
}
