package com.pusher.chatkit.messages.api

import com.pusher.chatkit.CustomData
import java.util.Date

internal data class MessageAttachmentBodyApiType(
    val id: String,
    val downloadUrl: String,
    val refreshUrl: String,
    val expiration: Date,
    val name: String?,
    val customData: CustomData?,
    val size: Int
)
