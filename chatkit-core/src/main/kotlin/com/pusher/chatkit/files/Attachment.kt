package com.pusher.chatkit.files

import com.google.gson.annotations.SerializedName

internal data class Attachment(
    @SerializedName("resource_link") val link: String,
    val type: String,
    val name: String
)
