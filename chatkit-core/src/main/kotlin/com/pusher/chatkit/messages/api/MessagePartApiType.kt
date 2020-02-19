package com.pusher.chatkit.messages.api

internal data class MessagePartApiType(
    val content: String?,
    val type: String,
    val url: String?,
    val attachment: MessageAttachmentApiType?
)
