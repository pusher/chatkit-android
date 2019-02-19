package com.pusher.chatkit.messages.multipart.request

import com.pusher.chatkit.CustomData

data class Message(
        val parts: List<Part>
)

sealed class Part {
    data class Inline @JvmOverloads constructor(
            val content: String,
            val type: String = "text/plain"
    ) : Part()

    data class Url(
            val url: String,
            val type: String
    ) : Part()

    data class Attachment @JvmOverloads constructor(
            val type: String,
            val attachment: AttachmentId,
            val name: String? = null,
            val customData: CustomData? = null
    ) : Part()
}

data class AttachmentId(val id: String)