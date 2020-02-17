package com.pusher.chatkit.files.api

internal sealed class AttachmentBodyApiType {
    data class Resource(val resourceLink: String, val type: String) : AttachmentBodyApiType()
    object None : AttachmentBodyApiType()
}
