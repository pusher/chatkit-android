package com.pusher.chatkit.presence

import com.pusher.chatkit.presence.api.UserPresenceApiType
import elements.Error

typealias PresenceSubscriptionConsumer = (PresenceSubscriptionEvent) -> Unit

sealed class PresenceSubscriptionEvent {
    internal data class PresenceUpdate(val presence: UserPresenceApiType) : PresenceSubscriptionEvent()
    internal data class ErrorOccurred(val error: Error) : PresenceSubscriptionEvent()
}
