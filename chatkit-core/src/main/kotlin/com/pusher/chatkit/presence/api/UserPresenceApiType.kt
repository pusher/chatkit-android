package com.pusher.chatkit.presence.api

import com.pusher.chatkit.presence.Presence

internal data class UserPresenceApiType(
        val presence: Presence,
        val userId: String
)
