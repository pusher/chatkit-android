package com.pusher.chatkit.users.api

import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.state.DeletedRoom
import com.pusher.chatkit.rooms.state.JoinedRoom
import com.pusher.chatkit.rooms.state.JoinedRoomsReceived
import com.pusher.chatkit.rooms.state.JoinedRoomsState
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

                val currentState: JoinedRoomsState? = stateGetter().joinedRoomsState
                if (currentState != null) {
                    determineRoomDifferences(currentState, event)
                } else {
                    dispatcher(JoinedRoomsReceived(
                            rooms = joinedRoomApiTypeMapper.toManyRoomInternal(event.rooms),
                            unreadCounts = joinedRoomApiTypeMapper.toManyUnreadCounts(event.readStates)
                    ))
                }
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

    private fun determineRoomDifferences(
        currentState: JoinedRoomsState,
        initialEvent: UserSubscriptionEvent.InitialState
    ) {

        determineNewRoomsJoined(currentState, initialEvent)

        determineNewRoomsLeft(currentState, initialEvent)

        // new updated rooms

        // new read counts
    }

    private fun determineNewRoomsJoined(
        currentState: JoinedRoomsState,
        initialEvent: UserSubscriptionEvent.InitialState
    ) {
        val addedRoomKeys = initialEvent.rooms.map { it.id }.toSet() - currentState.rooms.keys.toSet()

        for (addedKey in addedRoomKeys) {
            val room = initialEvent.rooms.find { it.id == addedKey }
            val unreadCount = initialEvent.readStates.find { it.roomId == addedKey }

            // todo: do we always get the unread count here?
            if (room != null &&
                    unreadCount != null) {
                dispatcher(JoinedRoom(
                        room = joinedRoomApiTypeMapper.toRoomInternal(room),
                        unreadCount = unreadCount.unreadCount))
            }
        }
    }

    private fun determineNewRoomsLeft(
        currentState: JoinedRoomsState,
        initialEvent: UserSubscriptionEvent.InitialState
    ) {
        val removedRoomKeys = currentState.rooms.keys.toSet() - initialEvent.rooms.map { it.id }.toSet()

        for (removedKey in removedRoomKeys) {
            dispatcher(LeftRoom(roomId = removedKey))
        }
    }
}
