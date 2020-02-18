package com.pusher.chatkit.messages.api

import java.util.Date

internal data class MessageApiType(
        val id: Int,
        val userId: String,
        val roomId: String,
        val parts: List<MessagePartApiType>,
        val createdAt: Date,
        val updatedAt: Date,
        val deletedAt: Date?
)
