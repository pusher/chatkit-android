package com.pusher.chatkit.users

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.presence.Presence

data class User(
    val id: String,
    val createdAt: String,
    var updatedAt: String,

    var name: String?,
    var avatarURL: String?,
    var customData: CustomData?,
    private var online: Boolean = false
) {

    var presence: Presence
        get() = if(online) Presence.Online else Presence.Offline
        set(value) { online = value === Presence.Online }

}
