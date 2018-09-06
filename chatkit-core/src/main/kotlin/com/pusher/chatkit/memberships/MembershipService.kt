package com.pusher.chatkit.memberships

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.PlatformClient

internal class MembershipService(
        private val chatManager: ChatManager,
        private val client: PlatformClient
) {
    fun subscribe(roomId: Int, consumeEvent: (ChatManagerEvent) -> Unit) =
        MembershipSubscription(
            roomId,
            client,
            chatManager,
            consumeEvent
        ).connect()
}