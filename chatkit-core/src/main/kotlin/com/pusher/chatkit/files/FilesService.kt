package com.pusher.chatkit.files

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import elements.Error
import java.util.concurrent.Future

internal class FilesService(
        private val client: PlatformClient
) {
    fun uploadFile(
            attachment: DataAttachment,
            roomId: Int,
            userId: String
    ): Result<AttachmentBody, Error> =
            client.upload(
                    "/rooms/$roomId/users/$userId/files/${attachment.name}",
                    attachment
            )

    fun fetchAttachment(
            attachmentUrl: String
    ): Result<FetchedAttachment, Error> =
            client.doRequest(
                    options = RequestOptions(
                        method = "GET",
                        destination = RequestDestination.Absolute(attachmentUrl)
                    ),
                    responseParser = { it.parseAs<FetchedAttachment>() }
            )
}