package com.pusher.chatkit.presence

data class UserPresence(
        val presence: Presence,
        val userId: String
)