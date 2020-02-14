package com.pusher.chatkit.messages.api

internal data class MessagePartBodyApiType(
        val content: String?,
        val type: String,
        val url: String?,
        val attachment: MessageAttachmentBodyApiType?
)