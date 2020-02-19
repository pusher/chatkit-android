package com.pusher.chatkit.messages.api

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
}
