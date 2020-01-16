package com.pusher.chatkit.model.network

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.rooms.api.RoomApiType
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType

/*
 * REQUESTS
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
 * RESPONSES
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
        val memberships: List<RoomMembershipApiType> // TODO: remove
)

internal data class JoinedRoomsResponse(
        val rooms: List<RoomApiType>,
        val memberships: List<RoomMembershipApiType>,
        val readStates: List<RoomReadStateApiType>
)
