package com.pusher.chatkit.presence

import com.pusher.chatkit.*
import com.pusher.chatkit.subscription.ChatkitSubscription
import kotlinx.coroutines.experimental.runBlocking

internal class PresenceService(private val chatManager: ChatManager) {
    fun subscribe(userId: String, consumeEvent: ChatManagerEventConsumer) =
        runBlocking {
            PresenceSubscription(userId, chatManager, consumeEvent).connect()
        }
}
