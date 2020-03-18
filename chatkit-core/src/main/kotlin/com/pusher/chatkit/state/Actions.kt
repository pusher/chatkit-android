package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.subscription.state.SubscriptionId
import elements.EosError

internal sealed class Action

internal data class JoinedRoomsReceived(
    val rooms: List<JoinedRoomInternalType>,
    val unreadCounts: Map<String, Int>
) : Action()

internal data class JoinedRoom(
    val room: JoinedRoomInternalType,
    val unreadCount: Int
) : Action()

internal data class LeftRoom(
    val roomId: String
) : Action()

internal data class RoomDeleted(
    val roomId: String
) : Action()

internal data class RoomUpdated(
    val room: JoinedRoomInternalType
) : Action()

internal data class ReconnectJoinedRoom(
    val room: JoinedRoomInternalType,
    val unreadCount: Int?
) : Action()

internal sealed class SubscriptionStateAction : Action() {
    internal data class Initializing(
        val subscriptionId: SubscriptionId
    ) : SubscriptionStateAction()

    internal data class Open(
        val subscriptionId: SubscriptionId
    ) : SubscriptionStateAction()

    internal data class Retrying(
        val subscriptionId: SubscriptionId
    ) : SubscriptionStateAction()

    internal data class Error(
        val subscriptionId: SubscriptionId,
        val error: elements.Error
    ) : SubscriptionStateAction()

    internal data class End(
        val subscriptionId: SubscriptionId,
        val error: EosError?
    ) : SubscriptionStateAction()
}
