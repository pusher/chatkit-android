package com.pusher.chatkit.presence.api

internal typealias PresenceSubscriptionConsumer = (PresenceSubscriptionEvent) -> Unit

internal data class PresenceSubscriptionEvent(
    val presence: UserPresenceApiType
)
