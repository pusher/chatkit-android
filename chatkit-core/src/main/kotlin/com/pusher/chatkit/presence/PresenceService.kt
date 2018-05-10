package com.pusher.chatkit.presence

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEventConsumer

internal class PresenceService(
    private val chatManager: ChatManager
) {

    fun subscribeToPresence(currentUserId: String, consumeEvent: ChatManagerEventConsumer) =
        PresenceSubscription(chatManager, currentUserId, consumeEvent)

}
