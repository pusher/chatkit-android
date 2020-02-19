package com.pusher.chatkit.messages.api

internal data class MessagePartApiType(
    val type: String,
    val content: String?,
    val url: String?
)
