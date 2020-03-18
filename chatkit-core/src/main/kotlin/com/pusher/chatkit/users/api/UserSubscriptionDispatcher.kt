package com.pusher.chatkit.users.api

import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.state.JoinedRoomsStateDiffer
import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.JoinedRoomsReceived
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.SubscriptionStateAction
import com.pusher.chatkit.subscription.SubscriptionListener
import com.pusher.chatkit.subscription.state.SubscriptionId
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
        dispatcher(SubscriptionStateAction.Initializing(SubscriptionId.UserSubscription))
    }

    override fun onOpen() {
        dispatcher(SubscriptionStateAction.Open(SubscriptionId.UserSubscription))
    }

    override fun onRetrying() {
        dispatcher(SubscriptionStateAction.Retrying(SubscriptionId.UserSubscription))
    }

    override fun onError(error: Error) {
        dispatcher(SubscriptionStateAction.Error(SubscriptionId.UserSubscription, error))
    }

    override fun onEnd(error: EosError?) {
        dispatcher(SubscriptionStateAction.End(SubscriptionId.UserSubscription, error))
    }
}
