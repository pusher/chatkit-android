package com.pusher.chatkit.messages.multipart

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.users.User
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import java.io.InputStream
import java.net.URL
import java.util.*


data class Message(
        val id: Int,
        val sender: User,
        val room: Room,
        val parts: List<Part>,
        val createdAt: Date,
        val updatedAt: Date
)

enum class PartType {
    InlinePayload,
    UrlPayload,
    AttachmentPayload
}

data class Part(
        val partType: PartType,
        val payload: Payload
)

sealed class Payload {
    data class Inline(
            val type: String,
            val content: String
    ) : Payload()

    data class Url(
            val type: String,
            val url: URL
    ) : Payload()

    class Attachment(
            val type: String,
            val size: Int,
            val name: String?,
            val customData: CustomData?,
            private val refresher: UrlRefresher,
            internal val refreshUrl: String,
            internal var downloadUrl: String,
            internal var expiration: Date
    ) : Payload() {
        fun url(): Result<String, Error> =
                if (Date().time - expiration.time > 30 * 60 * 1000) {
                    downloadUrl.asSuccess()
                } else {
                    refresher.refresh(this)
                }

        fun urlExpiry(): Date =
                expiration
    }
}

data class Attachment(
        val id: String,
        val downloadUrl: String,
        val refreshUrl: String,
        val expiration: Date,
        val name: String,
        val customData: CustomData,
        val size: Int
)

class UrlRefresher(
        private val client: PlatformClient
) {
    fun refresh(attachment: Payload.Attachment): Result<String, Error> =
            client.doRequest(RequestOptions(RequestDestination.Absolute(attachment.refreshUrl))) {
                it.parseAs<Attachment>()
            }.map { newAttachment ->
                attachment.downloadUrl = newAttachment.downloadUrl
                attachment.expiration = newAttachment.expiration

                newAttachment.downloadUrl
            }
}

sealed class NewPart {
    data class Inline @JvmOverloads constructor(
            val content: String,
            val type: String = "text/plain"
    ) : NewPart()

    data class Url(
            val url: String,
            val type: String
    ) : NewPart()

    data class Attachment @JvmOverloads constructor(
            val type: String,
            val file: InputStream,
            val name: String? = null,
            val customData: CustomData? = null
    ) : NewPart()
}