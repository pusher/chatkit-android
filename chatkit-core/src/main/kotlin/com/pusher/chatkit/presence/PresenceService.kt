package com.pusher.chatkit.presence

import com.pusher.chatkit.*
import com.pusher.chatkit.InstanceType.*
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.map
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.wait
import com.pusher.util.asFailure
import com.pusher.util.mapResult
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal class PresenceService(private val chatManager: ChatManager) {
    fun subscribe(userId: String, consumeEvent: ChatManagerEventConsumer): Subscription =
        PresenceSubscription(userId, chatManager, consumeEvent).connect()
}
