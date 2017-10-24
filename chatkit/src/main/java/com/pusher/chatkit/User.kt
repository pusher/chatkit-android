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
    fun updateWithPropertiesOfUser(user: User) {
        updatedAt = user.updatedAt
        name = user.name
        avatarURL = user.avatarURL
        customData = user.customData
        online = user.online
    }

}