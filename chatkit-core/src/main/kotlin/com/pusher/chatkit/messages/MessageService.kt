package com.pusher.chatkit.messages

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.files.DataAttachment
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.files.GenericAttachment
import com.pusher.chatkit.files.LinkAttachment
import com.pusher.chatkit.files.NoAttachment
import com.pusher.chatkit.files.api.AttachmentBodyApiType
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.toJson
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error

internal class MessageService(
    private val legacyV2client: PlatformClient,
    private val client: PlatformClient,
    private val userService: UserService,
    private val roomService: RoomService,
//    private val urlRefresher: UrlRefresher,
    private val filesService: FilesService
) {
    @Suppress("UNUSED_PARAMETER")
    fun fetchMessages(
        roomId: String,
        limit: Int,
        initialId: Int?,
        direction: Direction
    ) {}
//            : Result<List<Message>, Error> =
//            fetchMessagesParams(limit, initialId, direction).let { params ->
//                legacyV2client.doGet<List<Message>>("/rooms/$roomId/messages$params").flatMap { messages ->
//                    messages.map { message ->
//                        userService.fetchUserBy(message.userId).map { user ->
//                            message.user = user
//                            message
//                        }
//                    }.collect().mapFailure { errors ->
//                        Errors.compose(errors)
//                    }
//                }
//            }

    @Suppress("UNUSED_PARAMETER")
    fun fetchMultipartMessages(
        roomId: String,
        limit: Int,
        initialId: Int?,
        direction: Direction
    ) {}
//            : Result<List<com.pusher.chatkit.messages.multipart.Message>, Error> =
//            fetchMessagesParams(limit, initialId, direction).let { params ->
//                client.doGet<List<V3MessageBody>>("/rooms/$roomId/messages$params").flatMap { messages ->
//                    messages.map { message ->
//                        upgradeMessageV3(
//                                message,
//                                roomService,
//                                userService,
//                                urlRefresher
//                        )
//                    }.collect().mapFailure { errors ->
//                        Errors.compose(errors)
//                    }
//                }
//            }

    private fun fetchMessagesParams(limit: Int, initialId: Int?, direction: Direction) =
            listOfNotNull(
                    limit.takeIf { it > 0 }?.let { "limit" to it },
                    initialId?.let { "initial_id" to it },
                    "direction" to direction
            ).joinToString(separator = "&", prefix = "?") { (key, value) ->
                "$key=$value"
            }

    fun sendMessage(
        roomId: String,
        userId: String,
        text: String = "",
        attachment: GenericAttachment = NoAttachment
    ): Result<Int, Error> =
            attachment.asAttachmentBody(roomId, userId)
                    .flatMap { sendMessage(roomId, text, it) }

    @Suppress("UNUSED_PARAMETER")
    fun sendMultipartMessage(
        roomId: String
//        parts: List<NewPart>
    ) {}
//            : Result<Int, Error> =
//            parts.map {
//                toPartRequest(roomId, it)
//            }.collect().flatMap { requestParts ->
//                com.pusher.chatkit.messages.multipart.request.Message(requestParts)
//                        .toJson()
//                        .flatMap { body ->
//                            client.doPost<MessageSendingResponse>("/rooms/$roomId/messages", body)
//                        }.map {
//                            it.messageId
//                        }
//            }

    @Suppress("UNUSED_PARAMETER")
    private fun toPartRequest(
        roomId: String
//        part: NewPart
    ) {}
//            : Result<Part, Error> =
//            when (part) {
//                is NewPart.Inline ->
//                    Part.Inline(part.content, part.type).asSuccess()
//                is NewPart.Url ->
//                    Part.Url(part.url, part.type).asSuccess()
//                is NewPart.AttachmentApiType ->
//                    uploadAttachment(roomId, part).map { attachmentId ->
//                        Part.AttachmentApiType(part.type, AttachmentId(attachmentId), part.name, part.customData)
//                    }
//            }

    @Suppress("UNUSED_PARAMETER")
    private fun uploadAttachment(
        roomId: String
//        part: NewPart.AttachmentApiType
    ) {}
//            : Result<String, Error> {
//        val outputStream = ByteArrayOutputStream()
//        val length = part.file.copyTo(outputStream)
//
//        return AttachmentRequest(
//                contentType = part.type,
//                contentLength = length,
//                name = part.name,
//                customData = part.customData
//        ).toJson().flatMap { body ->
//            client.doPost<AttachmentResponse>(
//                    path = "/rooms/${URLEncoder.encode(roomId, "UTF-8")}/attachments",
//                    body = body
//            ).flatMap { attachmentResponse ->
//                client.externalUpload<Unit>(
//                        attachmentResponse.uploadUrl,
//                        mimeType = part.type,
//                        data = outputStream.toByteArray(),
//                        responseParser = { it.parseAs() }
//                ).map {
//                    attachmentResponse.attachmentId
//                }
//            }
//        }
//    }

    private data class AttachmentRequest(
        val contentType: String,
        val contentLength: Long,
        val name: String? = null
//        val customData: CustomData? = null
    )

    private data class AttachmentResponse(
        val attachmentId: String,
        val uploadUrl: String
    )

    private fun GenericAttachment.asAttachmentBody(
        roomId: String,
        userId: String
    ): Result<AttachmentBodyApiType, Error> =
            when (this) {
                is DataAttachment -> filesService.uploadFile(this, roomId, userId)
                is LinkAttachment -> AttachmentBodyApiType.Resource(link, type.toString())
                        .asSuccess()
                is NoAttachment -> AttachmentBodyApiType.None
                        .asSuccess()
            }

    private fun sendMessage(
        roomId: String,
        text: String = "",
        attachment: AttachmentBodyApiType
    ): Result<Int, Error> =
            MessageRequest(text, attachment.takeIf { it !== AttachmentBodyApiType.None })
                    .toJson()
                    .flatMap { body ->
                        legacyV2client.doPost<MessageSendingResponse>("/rooms/$roomId/messages", body)
                    }.map {
                        it.messageId
                    }
}

private data class MessageSendingResponse(
    val messageId: Int
)

private data class MessageRequest(
    val text: String? = null,
    val attachment: AttachmentBodyApiType? = null
)
