package com.pusher.chatkit.rooms.api

internal data class JoinRoomResponse(
        val room: RoomApiType,
        val membership: RoomMembershipApiType
)