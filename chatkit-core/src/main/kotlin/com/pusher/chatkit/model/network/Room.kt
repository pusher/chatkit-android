package com.pusher.chatkit.model.network

import com.pusher.chatkit.users.RoomApiType
import com.pusher.chatkit.users.RoomMembershipApiType

internal data class CreateRoomResponse(
        val room: RoomApiType,
        val members: RoomMembershipApiType
)

internal typealias JoinRoomResponse = CreateRoomResponse