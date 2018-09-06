package com.pusher.chatkit.presence

import elements.Error

sealed class PresenceSubscriptionEvent {
    internal data class InitialState(val userStates: List<UserPresence>): PresenceSubscriptionEvent()
    internal data class PresenceUpdate(val state: UserPresence): PresenceSubscriptionEvent()
    internal data class JoinedRoom(val userStates: List<UserPresence>): PresenceSubscriptionEvent()
    internal data class ErrorOccurred(val error: Error): PresenceSubscriptionEvent()
    internal object NoEvent: PresenceSubscriptionEvent()
}