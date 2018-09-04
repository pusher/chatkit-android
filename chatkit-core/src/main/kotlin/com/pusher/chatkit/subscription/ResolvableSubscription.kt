package com.pusher.chatkit.subscription

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.InstanceType
import com.pusher.chatkit.PlatformClient
import com.pusher.platform.SubscriptionListeners
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
    private val resolveOnFirstEvent: Boolean = false
): ChatkitSubscription {
    private lateinit var subscription: Subscription

    override fun connect(): ChatkitSubscription {
        val latch = CountDownLatch(1)
        subscription = client.subscribeResuming(
            path = this.path,
            listeners = SubscriptionListeners(
                onOpen = { headers ->
                    if (!resolveOnFirstEvent) {
                        latch.countDown()
                    }
                    listeners.onOpen(headers)
                },
                onEvent = { event ->
                    if (resolveOnFirstEvent) {
                        latch.countDown()
                    }
                    listeners.onEvent(event)
                },
                onError = { error ->
                    latch.countDown()
                    listeners.onError(error)
                },
                onEnd = { error ->
                    latch.countDown()
                    listeners.onEnd(error)
                },
                onSubscribe = listeners.onSubscribe,
                onRetrying = listeners.onRetrying
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
