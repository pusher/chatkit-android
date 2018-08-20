package com.pusher.chatkit.memberships

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import elements.Subscription

internal class MembershipService(private val chatManager: ChatManager) {
    fun subscribe(roomId: Int, consumeEvent: (ChatManagerEvent) -> Unit): Subscription {
        return MembershipSubscription(
            roomId,
            chatManager,
            consumeEvent
        ).connect()
    }
}