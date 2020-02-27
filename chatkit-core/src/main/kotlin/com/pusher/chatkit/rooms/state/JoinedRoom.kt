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

            state.with(JoinedRoomsState(
                    (state.joinedRoomsState!!.rooms + joinedRoom),
                    (state.joinedRoomsState.unreadCounts + joinedRoomUnreadCount)
            ))
        }
