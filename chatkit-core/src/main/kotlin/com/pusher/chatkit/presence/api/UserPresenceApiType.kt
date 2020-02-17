package com.pusher.chatkit.presence.api

internal data class UserPresenceApiType(
    val presence: PresenceApiType,
    val userId: String
)
