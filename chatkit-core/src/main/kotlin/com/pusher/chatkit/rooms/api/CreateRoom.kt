package com.pusher.chatkit.rooms.api

internal data class CreateRoomRequest(
    val id: String?,
    val name: String,
    val pushNotificationTitleOverride: String?,
    val private: Boolean,
    val createdById: String,
//    val customData: CustomData?,
    var userIds: List<String> = emptyList()
)

internal class CreateRoomResponse(
    val room: JoinedRoomApiType,
    val membership: RoomMembershipApiType
)
