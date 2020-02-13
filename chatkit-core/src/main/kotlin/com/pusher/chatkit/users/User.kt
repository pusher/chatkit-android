package com.pusher.chatkit.users

import com.google.gson.annotations.SerializedName
//import com.pusher.chatkit.CustomData
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.util.dateFormat
import java.util.Date

data class User(
    val id: String,
    private val createdAt: String,
    private val updatedAt: String,

    val name: String?,
    @SerializedName("avatar_url")
    val avatarURL: String?,
//    val customData: CustomData?,
    private var online: Boolean = false
) {
    var presence: Presence
        get() = if (online) Presence.Online else Presence.Offline
        set(value) {
            online = value === Presence.Online
        }

    val created: Date by lazy { dateFormat.parse(createdAt) }
    val updated: Date by lazy { dateFormat.parse(updatedAt) }
}
