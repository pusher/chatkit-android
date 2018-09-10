package com.pusher.chatkit.presence

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger

internal class PresenceSubscription(
    client: PlatformClient,
    userId: String,
    consumeEvent: PresenceSubscriptionConsumer,
    logger: Logger
): ChatkitSubscription {
    private var subscription = ResolvableSubscription(
            client = client,
            path = "/users/$userId/presence",
            listeners = SubscriptionListeners(
                    onEvent = { consumeEvent(it.body) },
                    onError = { error -> consumeEvent(PresenceSubscriptionEvent.ErrorOccurred(error)) }
            ),
            messageParser = PresenceSubscriptionEventParser,
            logger = logger,
            description = "Presence for user $userId",
            resolveOnFirstEvent = true
    )

    override fun connect(): ChatkitSubscription {
        return subscription.connect()
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}