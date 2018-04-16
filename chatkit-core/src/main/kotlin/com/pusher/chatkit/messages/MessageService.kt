package com.pusher.chatkit.messages

import com.pusher.annotations.UsesCoroutines
import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.*
import elements.Error
import elements.Errors
import elements.Subscription

typealias MessagesPromiseResult = Promise<Result<List<Message>, Error>>
typealias MessageIdPromiseResult = Promise<Result<Int, Error>>
typealias AttachmentPromiseResult = Promise<Result<AttachmentBody, Error>>

class MessageService(
    val room: Room,
    private val chatManager: ChatManager
) {

    private val tokenProvider get() = chatManager.tokenProvider
    private val tokenParams get() = chatManager.dependencies.tokenParams
    private val filesInstance get() = chatManager.filesInstance

    @JvmOverloads
    fun messageEvents(
        messageLimit: Int = 10,
        callback: (Result<Message, Error>) -> Unit
    ): Promise<Result<Subscription, Error>> = chatManager.currentUser
        .flatMapResult { currentUser -> messageEventsPath(currentUser, messageLimit).asPromise() }
        .mapResult { path ->
            val roomSubscription = RoomSubscription(room, chatManager.userStore, callback, chatManager)
            chatManager.apiInstance.subscribeResuming(
                path = path,
                tokenProvider = chatManager.tokenProvider,
                tokenParams = chatManager.dependencies.tokenParams,
                listeners = roomSubscription.subscriptionListeners
            )
        }

    private fun messageEventsPath(currentUser: CurrentUser, messageLimit: Int): Result<String, Error> = when {
        messageLimit < 0 -> Errors.other("messageLimit should be greater than 0").asFailure()
        else -> "/rooms/${room.id}?user_id=${currentUser.id}&message_limit=$messageLimit".asSuccess()
    }


    @JvmOverloads
    @UsesCoroutines
    fun messageEvents(messageLimit: Int = 10): Promise<Result<Message, Error>> =
        Promise.promise {
            val subscription = messageEvents(messageLimit) { report(it) }
            onCancel { subscription.cancel() }
        }

    // TODO: Create cursor service
//    fun cursors(callback: (CursorsSubscription.Event) -> Unit) {
//
//        val cursorsSubscription = CursorsSubscription(currentUser, room, chatManager, callback)
//        with(chatManager) {
//            cursorsInstance.subscribeResuming(
//                path = "/cursors/0/rooms/${room.id}/",
//                tokenProvider = tokenProvider,
//                tokenParams = tokenParams,
//                listeners = cursorsSubscription.subscriptionListeners
//            )
//        }
//    }

    fun fetchMessages(limit: Int = -1): MessagesPromiseResult =
        chatManager.doGet(when {
            limit > 0 -> "/rooms/${room.id}/messages?limit=$limit"
            else -> "/rooms/${room.id}/messages"
        }).parseResponseWhenReady()

    @JvmOverloads
    fun sendMessage(
        text: CharSequence = "",
        attachment: GenericAttachment = NoAttachment
    ): MessageIdPromiseResult =
        attachment.asAttachmentBody().flatMapResult { body -> sendMessage(text, body) }

    private fun GenericAttachment.asAttachmentBody(): AttachmentPromiseResult = when (this) {
        is DataAttachment -> uploadFile(this, room.id)
        is LinkAttachment -> AttachmentBody.Resource(link, type).asSuccess<AttachmentBody, elements.Error>().asPromise()
        is NoAttachment -> AttachmentBody.None.asSuccess<AttachmentBody, Error>().asPromise()
    }

    private fun uploadFile(
        attachment: DataAttachment,
        roomId: Int
    ): AttachmentPromiseResult = filesInstance.upload(
        path = "/rooms/$roomId/files/${attachment.name}",
        file = attachment.file,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).parseResponseWhenReady()

    private fun sendMessage(
        text: CharSequence = "",
        attachment: AttachmentBody
    ): MessageIdPromiseResult =
        chatManager.currentUser.flatMapResult { currentUser ->
            currentUser.sendMessage(text, attachment)
        }

    private fun CurrentUser.sendMessage(
        text: CharSequence = "",
        attachment: AttachmentBody
    ): MessageIdPromiseResult =
        MessageRequest(text.toString(), id, attachment.takeIf { it !== AttachmentBody.None })
            .toJson()
            .map { body -> chatManager.doPost("/rooms/${room.id}/messages", body) }
            .map { it.parseResponseWhenReady<MessageSendingResponse>() }
            .recover { error -> error.asFailure<MessageSendingResponse, Error>().asPromise() }
            .mapResult { it.messageId }

}

private data class MessageSendingResponse(val messageId: Int)
