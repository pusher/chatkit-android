package com.pusher.chatkit.api

internal enum class InstanceType(val serviceName: String, val version: String = "v1") {
    CORE("chatkit", "v7"),
    CURSORS("chatkit_cursors", "v2"),
    PRESENCE("chatkit_presence", "v2"),
    FILES("chatkit_files"),
    BEAMS_TOKEN_PROVIDER("chatkit_beams_token_provider")
}
