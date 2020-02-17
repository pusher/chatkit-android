package com.pusher.chatkit.files.api

import com.google.gson.annotations.SerializedName

internal data class AttachmentApiType(
    @SerializedName("resource_link") val link: String,
    val type: String,
    val name: String
)
