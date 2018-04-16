package com.pusher.chatkit

class User(
    val id: String,
    val createdAt: String,
    var updatedAt: String,

    var name: String?,
    var avatarURL: String?,
    var customData: CustomData?,
    var online: Boolean = false
) {

    sealed class Presence {
        object Online : Presence()
        object Offline : Presence()
    }

}
