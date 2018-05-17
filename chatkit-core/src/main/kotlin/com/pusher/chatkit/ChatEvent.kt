package com.pusher.chatkit

import com.google.gson.JsonElement
import com.pusher.chatkit.users.HasUser

internal data class ChatEvent(
    val eventName: String,
    override val userId: String = "",
    val timestamp: String,
    val data: JsonElement
) : HasUser
