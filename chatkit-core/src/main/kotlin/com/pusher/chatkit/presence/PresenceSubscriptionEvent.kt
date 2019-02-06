package com.pusher.chatkit.presence

import elements.Error

typealias PresenceSubscriptionConsumer = (PresenceSubscriptionEvent) -> Unit

sealed class PresenceSubscriptionEvent {
    internal data class PresenceUpdate(val presence: UserPresence) : PresenceSubscriptionEvent()
    internal data class ErrorOccurred(val error: Error) : PresenceSubscriptionEvent()
}