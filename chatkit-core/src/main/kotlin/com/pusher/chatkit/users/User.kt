package com.pusher.chatkit.users

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.util.dateFormat

data class User(
    val id: String,
    val createdAt: String,
    val updatedAt: String,

    val name: String?,
    val avatarURL: String?,
    val customData: CustomData?,
    private var online: Boolean = false
) {

    var presence: Presence
        get() = if(online) Presence.Online else Presence.Offline
        set(value) { online = value === Presence.Online }

    val created by lazy { dateFormat.parse(createdAt) }
    val updated by lazy { dateFormat.parse(updatedAt) }

}
