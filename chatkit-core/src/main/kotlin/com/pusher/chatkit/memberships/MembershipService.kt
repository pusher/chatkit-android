package com.pusher.chatkit.memberships

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent

internal class MembershipService(private val chatManager: ChatManager) {
    fun subscribe(roomId: Int, consumeEvent: (ChatManagerEvent) -> Unit) =
        MembershipSubscription(
            roomId,
            chatManager,
            consumeEvent
        ).connect()
}