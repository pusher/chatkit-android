package com.pusher.chatkit.files

import com.pusher.chatkit.AttachmentBody
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.FetchedAttachment
import com.pusher.chatkit.SERVICE_VERSION
import com.pusher.chatkit.network.parseAs
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import elements.Error
import java.util.concurrent.Future

private const val FILES_SERVICE_NAME = "chatkit_files"

internal class FilesService(private val chatManager: ChatManager) {

    private val filesInstance by chatManager.lazyInstance(FILES_SERVICE_NAME, SERVICE_VERSION)

    @Suppress("UNCHECKED_CAST")
    fun uploadFile(
        attachment: DataAttachment,
        roomId: Int,
        userId: String
    ): Future<Result<AttachmentBody, Error>> = filesInstance.upload(
        path = "/rooms/$roomId/users/$userId/files/${attachment.name}",
        file = attachment.file,
        tokenProvider = chatManager.tokenProvider,
        responseParser = { it.parseAs<AttachmentBody.Resource>() as Result<AttachmentBody, Error> }
    )

    fun fetchAttachment(attachmentUrl: String): Future<Result<FetchedAttachment, Error>> =
        filesInstance.request(
            options = RequestOptions(
                method = "GET",
                destination = RequestDestination.Absolute(attachmentUrl)
            ),
            tokenProvider = chatManager.tokenProvider,
            responseParser = { it.parseAs<FetchedAttachment>() }
        )

}
