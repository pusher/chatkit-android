package com.pusher.chatkit.presence

import com.pusher.chatkit.*
import com.pusher.chatkit.subscription.ChatkitSubscription

internal class PresenceService(private val chatManager: ChatManager) {
    fun subscribe(userId: String, consumeEvent: ChatManagerEventConsumer): ChatkitSubscription =
        PresenceSubscription(userId, chatManager, consumeEvent).connect()
}
