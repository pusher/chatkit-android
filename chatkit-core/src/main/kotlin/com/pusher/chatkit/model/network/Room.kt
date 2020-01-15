package com.pusher.chatkit.model.network

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.cursors.Cursor

/*
 * ENTITIES
 */
internal data class RoomApiType(
        val id: String,
        val createdById: String,
        val name: String,
        val pushNotificationTitleOverride: String?,
        val private: Boolean,
        val customData: CustomData?,
        val lastMessageAt: String?,
        val createdAt: String,
        val updatedAt: String,
        val deletedAt: String?
)

internal data class ReadStateApiType(
        val roomId: String,
        val unreadCount: Int,
        val cursor: Cursor?
)

internal data class RoomMembershipApiType(val roomId: String, val userIds: List<String>)

/*
 * SYNC REQUESTS
 */

internal data class UpdateRoomRequest(
        val name: String?,
        val private: Boolean?,
        val customData: CustomData?
)

internal data class UpdateRoomRequestWithPNTitleOverride(
        val name: String?,
        val pushNotificationTitleOverride: String?,
        val private: Boolean?,
        val customData: CustomData?
)

internal data class CreateRoomRequest(
        val id: String?,
        val name: String,
        val pushNotificationTitleOverride: String?,
        val private: Boolean,
        val createdById: String,
        val customData: CustomData?,
        var userIds: List<String> = emptyList()
)

/*
 * SYNC RESPONSES
 */
internal data class CreateRoomResponse(
        val room: RoomApiType,
        val membership: RoomMembershipApiType
)

// Create and Join room responses are type equivalent, because they both represent a room which
// the user must be a member of. Get room (below) is a distinct type, because it does not.
internal typealias JoinRoomResponse = CreateRoomResponse

internal data class GetRoomResponse(
    val room: RoomApiType,
    val membership: RoomMembershipApiType
)


internal data class JoinableRoomsResponse(
        val rooms: List<RoomApiType>,
        val memberships: List<RoomMembershipApiType>
)

internal data class JoinedRoomsResponse(
        val rooms: List<RoomApiType>,
        val memberships: List<RoomMembershipApiType>,
        val readStates: List<ReadStateApiType>
)
