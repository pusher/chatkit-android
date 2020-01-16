package com.pusher.chatkit.rooms.api

internal data class GetRoomResponse(
        val room: RoomApiType,
        val membership: RoomMembershipApiType
)