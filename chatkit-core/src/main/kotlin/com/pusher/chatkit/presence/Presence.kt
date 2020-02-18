package com.pusher.chatkit.presence

internal sealed class Presence {
    object Online : Presence()
    object Offline : Presence()
    object Unknown : Presence()
}
