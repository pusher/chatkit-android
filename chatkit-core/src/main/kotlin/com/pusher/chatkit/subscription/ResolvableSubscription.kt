package com.pusher.chatkit.subscription

import com.pusher.chatkit.PlatformClient
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.DataParser
import elements.Subscription
import java.util.concurrent.CountDownLatch

// Implements a subscription that can be used to resolve subscriptions
// either on opening a subscription (default behaviour) or
// on receiving the first event on the subscription
class ResolvableSubscription<A>(
    path: String,
    client: PlatformClient,
    listeners: SubscriptionListeners<A>,
    messageParser: DataParser<A>,
    logger: Logger,
    description: String,
    resolveOnFirstEvent: Boolean = false
) : Subscription {
    private val latch = CountDownLatch(1)

    private val subscription = loggingSubscription(
            path = path,
            listeners = SubscriptionListeners.compose(
                    listeners,
                    SubscriptionListeners(
                            onOpen = {
                                if (!resolveOnFirstEvent) {
                                    latch.countDown()
                                }
                            },
                            onEvent = {
                                if (resolveOnFirstEvent) {
                                    latch.countDown()
                                }
                            },
                            onError = {
                                latch.countDown()
                            },
                            onEnd = {
                                latch.countDown()
                            }
                    )
            ),
            messageParser = messageParser,
            client = client,
            logger = logger,
            description = description
    )

    fun await() {
        latch.await()
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}
