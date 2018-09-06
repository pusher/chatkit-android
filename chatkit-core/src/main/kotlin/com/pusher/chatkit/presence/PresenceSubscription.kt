package com.pusher.chatkit.presence

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import elements.Subscription

internal class PresenceSubscription(
    private val client: PlatformClient,
    private val userId: String,
    private val consumeEvent: (PresenceSubscriptionEvent) -> Unit,
    private val logger: Logger
): ChatkitSubscription {
    private lateinit var subscription: Subscription

    override fun connect(): ChatkitSubscription {
        subscription = ResolvableSubscription(
            client = client,
            path = "/users/$userId/presence",
            listeners = SubscriptionListeners(
                onOpen = { logger.verbose("[Presence] OnOpen triggered") },
                onEvent = {
                    logger.verbose("[Presence] Event received: $it")
                    consumeEvent(it.body)
                },
                onError = { error ->
                    logger.verbose("[Presence] Subscription error: $error")
                    consumeEvent(PresenceSubscriptionEvent.ErrorOccurred(error))
                },
                onEnd = { error -> logger.verbose("[Presence] Subscription ended with: $error") }
            ),
            messageParser = PresenceSubscriptionEventParser,
            resolveOnFirstEvent = true
        ).connect()

        return this
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}
