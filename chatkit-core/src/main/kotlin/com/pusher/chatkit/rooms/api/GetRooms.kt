package com.pusher.chatkit.rooms.api

internal data class JoinableRoomsResponse(
        val rooms: List<NotJoinedRoomApiType>
)

internal data class JoinedRoomsResponse(
        val rooms: List<JoinedRoomApiType>,
        val memberships: List<RoomMembershipApiType>,
        val readStates: List<RoomReadStateApiType>
)