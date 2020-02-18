package com.pusher.chatkit.presence

import com.pusher.chatkit.presence.api.UserPresenceApiType
import elements.Error

internal typealias PresenceSubscriptionConsumer = (PresenceSubscriptionEvent) -> Unit

internal data class PresenceSubscriptionEvent (
        val presence: UserPresenceApiType
)
