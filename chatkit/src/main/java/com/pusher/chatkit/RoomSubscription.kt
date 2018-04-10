package com.pusher.chatkit

import com.pusher.platform.SubscriptionListeners
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors
import elements.Headers
import elements.SubscriptionEvent
import java.net.URL


class RoomSubscription(
    val room: Room,
    private val userStore: GlobalUserStore,
    private val onEvent: (Result<Message, Error>) -> Unit,
    private val chatManager: ChatManager
) {

    val subscriptionListeners = SubscriptionListeners(
        onOpen = { handleOpen(it) },
        onEvent = { handleMessage(it) },
        onError = { handleError(it) }
    )

    private fun handleOpen(headers: Headers) {
        //TODO("Not handled currently.")
    }

    private fun handleMessage(event: SubscriptionEvent) {

        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)

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

            onEvent(message.asSuccess())
        } else {
            onEvent(Errors.other("Wrong event type: ${chatEvent.eventName}").asFailure())
        }

    }

    private fun handleError(error: Error) {
        onEvent(error.asFailure())
    }

}

