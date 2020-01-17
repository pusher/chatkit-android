package com.pusher.chatkit.rooms.api

import com.pusher.chatkit.CustomData

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