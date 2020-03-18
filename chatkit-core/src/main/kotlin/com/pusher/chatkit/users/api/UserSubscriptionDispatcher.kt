package com.pusher.chatkit.users.api

import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.state.JoinedRoomsStateDiffer
import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.JoinedRoomsReceived
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.subscription.SubscriptionListener
import elements.EosError
import elements.Error
import elements.SubscriptionEvent
import org.reduxkotlin.Dispatcher

internal class UserSubscriptionDispatcher(
    private val joinedRoomsStateDiffer: JoinedRoomsStateDiffer,
    private val joinedRoomApiTypeMapper: JoinedRoomApiTypeMapper,
    private val dispatcher: Dispatcher
) : SubscriptionListener<UserSubscriptionEvent> {

    override fun onEvent(elementsEvent: SubscriptionEvent<UserSubscriptionEvent>) {
        when (val event = elementsEvent.body) {
            is UserSubscriptionEvent.InitialState ->
                if (joinedRoomsStateDiffer.stateExists()) {
                    joinedRoomsStateDiffer.toActions(
                        joinedRoomApiTypeMapper.toRoomInternalTypes(event.rooms),
                        joinedRoomApiTypeMapper.toUnreadCounts(event.readStates)
                    ).forEach {
                        action -> dispatcher(action)
                    }
                } else {
                    dispatcher(
                        JoinedRoomsReceived(
                            joinedRoomApiTypeMapper.toRoomInternalTypes(event.rooms),
                            joinedRoomApiTypeMapper.toUnreadCounts(event.readStates)
                        )
                    )
                }
            is UserSubscriptionEvent.AddedToRoomEvent ->
                dispatcher(
                    JoinedRoom(
                        joinedRoomApiTypeMapper.toRoomInternalType(event.room),
                        event.readState.unreadCount
                    )
                )
            is UserSubscriptionEvent.RemovedFromRoomEvent ->
                dispatcher(LeftRoom(event.roomId))
            is UserSubscriptionEvent.RoomUpdatedEvent ->
                dispatcher(RoomUpdated(joinedRoomApiTypeMapper.toRoomInternalType(event.room)))
            is UserSubscriptionEvent.RoomDeletedEvent ->
                dispatcher(RoomDeleted(event.roomId))
        }
    }

    override fun onSubscribe() {
        // Not yet implemented
    }

    override fun onOpen() {
        // Not yet implemented
    }

    override fun onRetrying() {
        // Not yet implemented
    }

    override fun onError(error: Error) {
        // Not yet implemented
    }

    override fun onEnd(error: EosError?) {
        // Not yet implemented
    }
}
