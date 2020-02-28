package com.pusher.chatkit.users.api

import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.state.DeletedRoom
import com.pusher.chatkit.rooms.state.JoinedRoom
import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.rooms.state.JoinedRoomsReceived
import com.pusher.chatkit.rooms.state.LeftRoom
import com.pusher.chatkit.rooms.state.UpdatedRoom
import com.pusher.chatkit.state.ChatkitState
import org.reduxkotlin.Dispatcher
import org.reduxkotlin.GetState

internal class UserSubscriptionDispatcher(
    val stateGetter: GetState<ChatkitState>,
    val joinedRoomApiTypeMapper: JoinedRoomApiTypeMapper,
    val dispatcher: Dispatcher
) {

    internal fun onEvent(event: UserSubscriptionEvent) {
        when (event) {
            is UserSubscriptionEvent.InitialState -> {

                dispatcher(JoinedRoomsReceived(
                        rooms = joinedRoomApiTypeMapper.toManyRoomInternal(event.rooms),
                        unreadCounts = joinedRoomApiTypeMapper.toManyUnreadCounts(event.readStates)
                ))

                // todo - also check if there's already an initial state and calculate the difference
            }
            is UserSubscriptionEvent.AddedToRoomEvent -> {
                dispatcher(JoinedRoom(
                        room = joinedRoomApiTypeMapper.toRoomInternal(event.room),
                        unreadCount = event.readState.unreadCount))
            }
            is UserSubscriptionEvent.RemovedFromRoomEvent -> {
                dispatcher(LeftRoom(roomId = event.roomId))
            }
            is UserSubscriptionEvent.RoomUpdatedEvent -> {
                dispatcher(UpdatedRoom(room = joinedRoomApiTypeMapper.toRoomInternal(event.room)))
            }
            is UserSubscriptionEvent.RoomDeletedEvent -> {
                dispatcher(DeletedRoom(roomId = event.roomId))
            }
        }
    }
}
