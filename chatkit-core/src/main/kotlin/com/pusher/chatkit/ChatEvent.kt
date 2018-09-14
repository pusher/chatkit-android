package com.pusher.chatkit

import com.google.gson.JsonElement

internal data class ChatEvent(
    val eventName: String,
    val userId: String = "",
    val timestamp: String,
    val data: JsonElement
)
