package com.pusher.chatkit.presence

import com.pusher.chatkit.*

internal class PresenceService(private val chatManager: ChatManager) {
    fun subscribe(userId: String, consumeEvent: ChatManagerEventConsumer) =
            PresenceSubscription(userId, chatManager, consumeEvent).connect()
}
