package com.pusher.chatkit

import com.google.gson.annotations.SerializedName

data class Room(
    val id: Int,
    val createdById: String,
    var name: String,
    var isPrivate: Boolean,
    val createdAt: String,
    var updatedAt: String,
    var deletedAt: String
) {

    @SerializedName("member_user_ids") private var _memberUserIds: MutableList<String>? = null
    val memberUserIds: List<String>
        get() = _memberUserIds ?: mutableListOf<String>().also { _memberUserIds = it }

    private var _userStore : UserStore? = null
    val userStore: UserStore
        get() = _userStore ?: UserStore().also { _userStore = it }

    fun removeUser(userId: String) {
        _memberUserIds?.remove(userId)
        userStore.remove(userId)
    }

    fun updateWithPropertiesOfRoom(updatedRoom: Room) {
        name = updatedRoom.name
        isPrivate = updatedRoom.isPrivate
        updatedAt = updatedRoom.updatedAt
        deletedAt = updatedRoom.deletedAt
        _memberUserIds = updatedRoom._memberUserIds
    }

}
