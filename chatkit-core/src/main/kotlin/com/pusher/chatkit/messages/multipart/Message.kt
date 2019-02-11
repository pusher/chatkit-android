package com.pusher.chatkit.messages.multipart

import com.pusher.chatkit.CustomData
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.users.User
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.Instance
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import java.net.URL
import java.time.LocalDateTime
import java.util.*


data class Message(
        val id: Int,
        val senderId: String,
        val roomId: String,
        val parts: List<Part>,
        val createdAt: Date,
        val updatedAt: Date
) {
    var sender: User? = null
    var room: Room? = null
}

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
    data class InlinePayload(
            val type: String,
            val content: String
    ) : Payload()

    data class UrlPayload(
            val type: String,
            val url: URL
    ) : Payload()

    class AttachmentPayload(
            val type: String,
            val size: Int,
            val name: String?,
            val customData: CustomData,
            private val refresher: UrlRefresher,
            internal val refreshUrl: String,
            internal var downloadUrl: String,
            internal var expiration: Date
    ) : Payload() {
        fun url(): Result<String, Error> =
                if (Date().after(expiration)) {
                    downloadUrl.asSuccess()
                } else {
                    refresher.refresh(this).map { downloadUrl }
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
    fun refresh(attachment: Payload.AttachmentPayload) =
            client.doRequest(
                    RequestOptions(RequestDestination.Absolute(attachment.refreshUrl)),
                    { it.parseAs<Attachment>() }
            ).map { newAttachment ->
                attachment.downloadUrl = newAttachment.downloadUrl
                attachment.expiration = newAttachment.expiration
            }
}
