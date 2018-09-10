package com.pusher.chatkit.cursors

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger

class CursorSubscription(
        client: PlatformClient,
        path: String,
        private val consumers: List<CursorSubscriptionConsumer>,
        private val logger: Logger
): ChatkitSubscription {
    private var subscription = ResolvableSubscription(
            client = client,
            path = path,
            listeners = SubscriptionListeners(
                    onOpen = { logger.verbose("[Cursor] OnOpen triggered") },
                    onEvent = { event ->
                        consumers.forEach { consumer -> consumer(event.body) }
                    },
                    onError = { error ->
                        logger.verbose("[Cursor] Subscription error: $error")
                        consumers.forEach { consumer -> consumer(CursorSubscriptionEvent.OnError(error)) }
                    },
                    onSubscribe = { logger.verbose("[Cursor] Subscription established") },
                    onRetrying = { logger.verbose("[Cursor] Subscription lost. Trying again.") },
                    onEnd = { error -> logger.verbose("[Cursor] Subscription ended with: $error") }
            ),
            messageParser = CursorSubscriptionEventParser
    )

    override fun connect(): ChatkitSubscription{
        return subscription.connect()
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}