package com.pusher.chatkit.rooms.api

internal data class JoinRoomResponse(
        val room: JoinedRoomApiType,
        val membership: RoomMembershipApiType
)