package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.CustomData

internal data class CreateRoomRequest(
        val id: String?,
        val name: String,
        val pushNotificationTitleOverride: String?,
        val private: Boolean,
        val createdById: String,
        val customData: CustomData?,
        var userIds: List<String> = emptyList()
)

// when a user creates a room then they automatically join the room, the response format is the same
internal typealias CreateRoomResponse = JoinRoomResponse