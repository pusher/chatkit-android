package com.pusher.chatkit.memberships

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.subscription.ChatkitSubscription
import kotlinx.coroutines.experimental.runBlocking

internal class MembershipService(private val chatManager: ChatManager) {
    fun subscribe(roomId: Int, consumeEvent: (ChatManagerEvent) -> Unit) =
        runBlocking {
            MembershipSubscription(
                roomId,
                chatManager,
                consumeEvent
            ).connect()
        }
}