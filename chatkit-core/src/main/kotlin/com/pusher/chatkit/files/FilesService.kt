package com.pusher.chatkit.files

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.InstanceType
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import elements.Error
import java.util.concurrent.Future

internal class FilesService(private val chatManager: ChatManager) {

    fun uploadFile(
        attachment: DataAttachment,
        roomId: Int,
        userId: String
    ): Future<Result<AttachmentBody, Error>> =
        chatManager.upload("/rooms/$roomId/users/$userId/files/${attachment.name}", attachment)

    // TODO: [platformInstance] is exposed just because of this, need to find a better way to do
    // absolute and relative paths that doesn't mean duplicating all the request methods.
    fun fetchAttachment(attachmentUrl: String): Future<Result<FetchedAttachment, Error>> =
        chatManager.platformInstance(InstanceType.FILES).request(
            options = RequestOptions(
                method = "GET",
                destination = RequestDestination.Absolute(attachmentUrl)
            ),
            tokenProvider = chatManager.tokenProvider,
            responseParser = { it.parseAs<FetchedAttachment>() }
        )

}
