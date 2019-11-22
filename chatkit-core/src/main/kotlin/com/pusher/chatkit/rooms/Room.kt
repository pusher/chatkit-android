package com.pusher.chatkit.rooms

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.CustomData

data class Room(
        val id: String,
        val createdById: String,
        var name: String,
        var pushNotificationTitleOverride: String?,
        @SerializedName("private")
        var isPrivate: Boolean,
        var customData: CustomData?,
        val unreadCount: Int?,
        val lastMessageAt: String?,
        val createdAt: String,
        var updatedAt: String,
        var deletedAt: String?
) {
    @SerializedName("member_user_ids")
    private var _memberUserIds: MutableSet<String>? = null
    val memberUserIds: Set<String>
        get() = memberUserIds()

    private fun memberUserIds(): MutableSet<String> = _memberUserIds
            ?: mutableSetOf<String>().also { _memberUserIds = it }

    internal fun removeUser(userId: String) {
        memberUserIds() -= userId
    }

    internal fun addUser(userId: String) {
        memberUserIds() += userId
    }

    internal fun addAllUsers(userIds: Set<String>) {
        memberUserIds().addAll(userIds)
    }

    override fun equals(other: Any?) = (other is Room) && id == other.id

    override fun hashCode(): Int { return id.hashCode() }

    fun deepEquals(other: Room) =
            name == other.name &&
                    pushNotificationTitleOverride == other.pushNotificationTitleOverride &&
                    customData == other.customData &&
                    isPrivate == other.isPrivate  &&
                    unreadCount == other.unreadCount &&
                    lastMessageAt == other.lastMessageAt

    fun withUnreadCount(unreadCount: Int) =
        Room(id, createdById, name, pushNotificationTitleOverride, isPrivate, customData,
                unreadCount, lastMessageAt, createdAt, updatedAt, deletedAt)

}
