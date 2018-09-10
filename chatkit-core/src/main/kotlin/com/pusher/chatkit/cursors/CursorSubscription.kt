package com.pusher.chatkit.cursors

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger


class CursorSubscription(
        client: PlatformClient,
        path: String,
        consumers: List<CursorSubscriptionConsumer>,
        logger: Logger
): ChatkitSubscription {
    private var subscription = ResolvableSubscription(
            client = client,
            path = path,
            listeners = SubscriptionListeners(
                    onEvent = { event -> consumers.forEach { consumer -> consumer(event.body) } },
                    onError = { error -> consumers.forEach { consumer -> consumer(CursorSubscriptionEvent.OnError(error)) } }
            ),
            messageParser = CursorSubscriptionEventParser,
            description = "Cursor $path",
            logger = logger
    )

    override fun connect(): ChatkitSubscription{
        return subscription.connect()
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}