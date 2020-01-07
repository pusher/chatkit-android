package com.pusher.chatkit.rooms

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.RoomApiType
import com.pusher.chatkit.users.RoomMembershipApiType

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
        internal constructor(
                room: RoomApiType,
                memberships: Set<String>?,
                unreadCount: Int?
        ) : this(
                id = room.id,
                name = room.name,
                createdById = room.createdById,
                pushNotificationTitleOverride = room.pushNotificationTitleOverride,
                isPrivate = room.private,
                customData = room.customData,
                createdAt = room.createdAt,
                updatedAt = room.updatedAt,
                deletedAt = room.deletedAt,
                lastMessageAt = room.lastMessageAt,
                unreadCount = unreadCount,
                memberUserIds = memberships.orEmpty()
        )
}
