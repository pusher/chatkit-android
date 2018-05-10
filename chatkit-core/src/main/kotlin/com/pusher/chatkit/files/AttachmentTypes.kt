package com.pusher.chatkit.files

import java.io.File

sealed class GenericAttachment

data class DataAttachment @JvmOverloads constructor(
    val file: File,
    val name: String = "file"
): GenericAttachment()

data class LinkAttachment(val link: String, val type: AttachmentType): GenericAttachment()

enum class AttachmentType {
    IMAGE, VIDEO, AUDIO, FILE;

    companion object {
        operator fun invoke(value: String) = when(value) {
            "image" -> IMAGE
            "video" -> VIDEO
            "audio" -> AUDIO
            else -> FILE
        }
    }

    override fun toString() = when(this) {
        IMAGE -> "image"
        VIDEO -> "video"
        AUDIO -> "audio"
        FILE -> "file"
    }
}

object NoAttachment : GenericAttachment()
