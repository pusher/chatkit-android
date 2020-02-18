package com.pusher.chatkit.presence.api

import com.pusher.chatkit.presence.Presence

internal data class UserPresenceApiType(
        val userId: String,
        val presence: Presence
)
