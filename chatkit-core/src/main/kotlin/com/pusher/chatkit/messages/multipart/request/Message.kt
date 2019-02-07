package com.pusher.chatkit.messages.multipart.request

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
}
