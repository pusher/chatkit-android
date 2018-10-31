package com.pusher.chatkit.rooms

import com.google.gson.annotations.SerializedName

data class Room(
    val id: String,
    val createdById: String,
    var name: String,
    @SerializedName("private")
    var isPrivate: Boolean,
    val createdAt: String,
    var updatedAt: String,
    var deletedAt: String
) {

    @SerializedName("member_user_ids") private var _memberUserIds: MutableSet<String>? = null
    val memberUserIds: Set<String>
        get() = memberUserIds()

    private fun memberUserIds(): MutableSet<String> = _memberUserIds
        ?: mutableSetOf<String>().also { _memberUserIds = it }

    fun removeUser(userId: String) {
        memberUserIds() -= userId
    }

    fun addUser(userId: String) {
        memberUserIds() += userId
    }
}
