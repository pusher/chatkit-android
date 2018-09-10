package com.pusher.chatkit.presence

import elements.Error

typealias PresenceSubscriptionConsumer = (PresenceSubscriptionEvent) -> Unit

sealed class PresenceSubscriptionEvent {
    internal data class InitialState(val userStates: List<UserPresence>): PresenceSubscriptionEvent()
    internal data class PresenceUpdate(val state: UserPresence): PresenceSubscriptionEvent()
    internal data class JoinedRoom(val userStates: List<UserPresence>): PresenceSubscriptionEvent()
    internal data class ErrorOccurred(val error: Error): PresenceSubscriptionEvent()
}