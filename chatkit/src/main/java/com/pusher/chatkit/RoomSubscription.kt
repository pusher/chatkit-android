package com.pusher.chatkit

import android.os.Handler
import android.os.Looper
import com.pusher.platform.SubscriptionListeners
import elements.Error
import elements.Headers
import elements.SubscriptionEvent
import java.net.URL
import java.net.URLDecoder
import java.util.*


class RoomSubscription(user: CurrentUser, val room: Room, val userStore: GlobalUserStore, val listeners: RoomSubscriptionListeners) {

    val mainThread: Handler = Handler(Looper.getMainLooper())

    val subscriptionListeners = SubscriptionListeners(
            onOpen = { handleOpen(it) },
            onEvent = { handleMessage(it) },
            onError = { handleError(it) }
    )

    fun handleOpen(headers: Headers) {
        //TODO("Not handled currently.")
    }

    fun handleMessage(event: SubscriptionEvent) {

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

            message.room = room
            userStore.fetchUsersWithIds(
                    userIds = setOf(message.userId),
                    onComplete = UsersListener { users ->
                        if (users.isNotEmpty())
                            message.user = users[0]
                        mainThread.post { listeners.onNewMessage(message) }

                    },
                    onFailure = ErrorListener {
                        mainThread.post { listeners.onNewMessage(message) }
                    })
        } else {
            TODO("Some weird shit has happened. Event received is of the wrong type ${chatEvent.eventName}")
        }

    }

    fun handleError(error: Error) {
        mainThread.post { listeners.onError(error) }
    }

}
