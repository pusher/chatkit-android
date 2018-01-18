package com.pusher.chatkit

import java.io.File

sealed class GenericAttachment

data class DataAttachment(val file: File, val name: String): GenericAttachment()

data class LinkAttachment(val link: String, val type: String): GenericAttachment()
