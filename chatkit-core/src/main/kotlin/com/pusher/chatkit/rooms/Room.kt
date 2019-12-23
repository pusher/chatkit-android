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
        var deletedAt: String?,
        val memberUserIds: Set<String>
) {

    override fun equals(other: Any?) = (other is Room) && id == other.id

    override fun hashCode(): Int { return id.hashCode() }

    fun deepEquals(other: Room) =
            name == other.name &&
                    pushNotificationTitleOverride == other.pushNotificationTitleOverride &&
                    customData == other.customData &&
                    isPrivate == other.isPrivate  &&
                    unreadCount == other.unreadCount &&
                    lastMessageAt == other.lastMessageAt

    fun withAddedMember(addedMemberId: String) = copy(memberUserIds = memberUserIds + addedMemberId)

    fun withLeftMember(leftMemberId: String) = copy(memberUserIds = memberUserIds - leftMemberId)

}
