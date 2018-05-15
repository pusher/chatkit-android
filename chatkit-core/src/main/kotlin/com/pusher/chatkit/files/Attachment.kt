package com.pusher.chatkit.files

import com.google.gson.annotations.SerializedName

data class Attachment(
    @Transient var fetchRequired: Boolean = false,
    @SerializedName("resource_link") val link: String,
    val type: String
)
