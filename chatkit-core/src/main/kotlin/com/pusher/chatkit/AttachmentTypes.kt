package com.pusher.chatkit

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
        AttachmentType.IMAGE -> "image"
        AttachmentType.VIDEO -> "video"
        AttachmentType.AUDIO -> "audio"
        AttachmentType.FILE -> "file"
    }
}

object NoAttachment : GenericAttachment()
