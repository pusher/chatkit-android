package com.pusher.chatkit.subscription

import com.pusher.chatkit.PlatformClient
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.loggingListeners
import com.pusher.platform.network.DataParser
import elements.Subscription
import java.util.concurrent.CountDownLatch

// Implements a subscription that can be used to resolve subscriptions
// either on opening a subscription (default behaviour) or
// on receiving the first event on the subscription
internal class ResolvableSubscription<A>(
    private val path: String,
    private val client: PlatformClient,
    private val listeners: SubscriptionListeners<A>,
    private val messageParser: DataParser<A>,
    private val logger: Logger,
    private val description: String,
    private val resolveOnFirstEvent: Boolean = false
): ChatkitSubscription {
    private lateinit var subscription: Subscription

    override fun connect(): ChatkitSubscription {
        val latch = CountDownLatch(1)
        subscription = client.subscribeResuming(
            path = this.path,
            listeners = SubscriptionListeners.compose(
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
                        ),
                    loggingListeners(description, logger),
                    listeners
            ),
            messageParser = messageParser
        )
        latch.await()

        return this
    }

    override fun unsubscribe() {
        this.subscription.unsubscribe()
    }
}
