package com.pusher.chatkit.presence.api

sealed class PresenceApiType {
    object Online : PresenceApiType()
    object Offline : PresenceApiType()
    object Unknown : PresenceApiType()
}
