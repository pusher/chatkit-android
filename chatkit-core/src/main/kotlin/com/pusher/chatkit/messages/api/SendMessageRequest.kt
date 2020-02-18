package com.pusher.chatkit.messages.api

import com.pusher.chatkit.CustomData

internal data class SendMessageRequest(
    val parts: List<SendMessageRequestPart>
)

internal sealed class SendMessageRequestPart {
    data class Inline @JvmOverloads constructor(
        val content: String,
        val type: String = "text/plain"
    ) : SendMessageRequestPart()

    data class Url(
        val url: String,
        val type: String
    ) : SendMessageRequestPart()

    data class Attachment @JvmOverloads constructor(
        val type: String,
        val attachment: AttachmentId,
        val name: String? = null,
        val customData: CustomData? = null
    ) : SendMessageRequestPart()
}

internal data class AttachmentId(val id: String)
