package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.network.wait
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import elements.emptyHeaders

private const val USERS_PATH = "users"

internal class UserSubscription(
    private val client: PlatformClient,
    private val consumeEvent: (UserSubscriptionEvent) -> Unit,
    private val logger: Logger
) : ChatkitSubscription {
    private var headers: Headers = emptyHeaders()

    private lateinit var underlyingSubscription: Subscription

    override fun connect(): ChatkitSubscription {
        val deferredUserSubscription = Futures.schedule {
            ResolvableSubscription(
                client = client,
                path = USERS_PATH,
                listeners = SubscriptionListeners(
                    onOpen = { headers ->
                        logger.verbose("[User] OnOpen $headers")
                        this@UserSubscription.headers = headers
                    },
                    onEvent = { event: SubscriptionEvent<UserSubscriptionEvent> ->
                        event.body
                            .also(consumeEvent)
                            .also { logger.verbose("[User] Event received $it") }
                    },
                    onError = { error -> consumeEvent(UserSubscriptionEvent.ErrorOccurred(error)) },
                    onSubscribe = { logger.verbose("[User] Subscription established.") },
                    onRetrying = { logger.verbose("[User] Subscription lost. Trying again.") },
                    onEnd = { error -> logger.verbose("[User] Subscription ended with: $error") }
                ),
                messageParser = UserSubscriptionEventParser,
                resolveOnFirstEvent = true
            ).connect()
        }

        underlyingSubscription = deferredUserSubscription.wait()

        return this
    }

    override fun unsubscribe() {
        underlyingSubscription.unsubscribe()
    }
}