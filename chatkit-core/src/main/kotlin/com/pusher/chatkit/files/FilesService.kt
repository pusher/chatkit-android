package com.pusher.chatkit.files

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import elements.Error
import java.net.URLEncoder
import java.util.concurrent.Future

internal class FilesService(
        private val client: PlatformClient
) {
    fun uploadFile(
            attachment: DataAttachment,
            roomId: String,
            userId: String
    ): Result<AttachmentBody, Error> =
            client.upload(
                    "/rooms/${URLEncoder.encode(roomId, "UTF-8")}/users/${URLEncoder.encode(userId, "UTF-8")}/files/${URLEncoder.encode(attachment.name, "UTF-8")}",
                    attachment
            )
}