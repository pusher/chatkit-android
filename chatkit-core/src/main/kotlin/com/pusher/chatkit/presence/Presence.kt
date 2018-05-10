package com.pusher.chatkit.presence

sealed class Presence {
    object Online : Presence()
    object Offline : Presence()
}
