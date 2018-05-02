package com.pusher.chatkit

import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.typeToken
import com.pusher.platform.SubscriptionListeners
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors
import elements.Subscription
import elements.SubscriptionEvent
import java.net.URL

class RoomSubscription internal constructor(
    room: Room,
    userId: String,
    private val listeners: RoomSubscriptionListeners,
    chatManager: ChatManager,
    messageLimit: Int
) : Subscription {

    private val roomId = room.id

    private val subscription = chatManager.apiInstance.subscribeResuming(
        path = "/rooms/$roomId?user_id=$userId&message_limit=$messageLimit",
        tokenProvider = chatManager.tokenProvider,
        tokenParams = chatManager.dependencies.tokenParams,
        listeners = SubscriptionListeners(
            onOpen = { }, //TODO("Not handled currently.")
            onEvent = ::handleMessage,
            onError = ::handleError
        ),
        bodyParser = { it.parseAs() }
    )

    init {
        check(messageLimit > 0) { "messageLimit should be greater than 0" }
    }

    private fun handleMessage(event: SubscriptionEvent<ChatEvent>) {

        val chatEvent = event.body

        if (chatEvent.eventName == "new_message") {

            val message = ChatManager.GSON.fromJson<Message>(chatEvent.data, Message::class.java)

            if (message.attachment != null) {
                val attachmentURL = URL(message.attachment.link)
                val queryParamsMap: MutableMap<String, String> = mutableMapOf()
                attachmentURL.query.split("&").forEach { pair ->
                    val splitPair = pair.split("=")
                    if (splitPair.count() == 2) {
                        queryParamsMap[splitPair[0]] = splitPair[1]
                    }
                }
                if (queryParamsMap["chatkit_link"] == "true") {
                    message.attachment.fetchRequired = true
                }
            }

            listeners.onNewMessage(message)
        } else {
            listeners.onError(Errors.other("Wrong event type: ${chatEvent.eventName}"))
        }

    }

    private fun handleError(error: Error) {
        listeners.onError(error)
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }

}

