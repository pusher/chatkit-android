package com.pusher.chatkit.presence

data class UserPresence(
        private val state: String,
        val lastSeenAt: String,
        val userId: String
) {
    val presence: Presence
        get() = when(state) {
            "online" -> Presence.Online
            else -> Presence.Offline
        }
}
