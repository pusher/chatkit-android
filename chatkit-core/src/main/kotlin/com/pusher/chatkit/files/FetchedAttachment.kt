package com.pusher.chatkit.files

import com.google.gson.annotations.SerializedName

data class FetchedAttachment(
    val file: FetchedAttachmentFile,
    @SerializedName("resource_link") val link: String,
    val ttl: Double
)

data class FetchedAttachmentFile(val bytes: Int, val lastModified: Double, val name: String)
