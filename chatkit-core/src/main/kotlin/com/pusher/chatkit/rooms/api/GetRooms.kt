package com.pusher.chatkit.rooms.api

internal data class JoinableRoomsResponse(
        val rooms: List<RoomApiType>,
        val memberships: List<RoomMembershipApiType> // TODO: remove
)

internal data class JoinedRoomsResponse(
        val rooms: List<RoomApiType>,
        val memberships: List<RoomMembershipApiType>,
        val readStates: List<RoomReadStateApiType>
)