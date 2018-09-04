package com.pusher.chatkit.presence

import elements.Error

sealed class PresenceSubscriptionEvent {
    internal data class InitialState(val presences: List<UserPresence>): PresenceSubscriptionEvent()
    internal data class PresenceUpdate(val presence: UserPresence): PresenceSubscriptionEvent()
    internal data class JoinedRoom(val presences: List<UserPresence>): PresenceSubscriptionEvent()
    internal data class ErrorOccurred(val error: Error): PresenceSubscriptionEvent()
    internal object NoEvent: PresenceSubscriptionEvent()
}