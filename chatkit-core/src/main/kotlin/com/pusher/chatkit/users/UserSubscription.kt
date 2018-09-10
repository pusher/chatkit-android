package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger

private const val USERS_PATH = "users"

internal class UserSubscription(
    client: PlatformClient,
    consumeEvent: UserSubscriptionConsumer,
    logger: Logger
) : ChatkitSubscription {
    private var underlyingSubscription = ResolvableSubscription(
            client = client,
            path = USERS_PATH,
            listeners = SubscriptionListeners(
                    onOpen = { logger.verbose("[User] OnOpen triggered") },
                    onEvent = { event ->
                        event.body
                                .also(consumeEvent)
                                .also { logger.verbose("[User] Event received $it") }
                    },
                    onError = { error ->
                        logger.verbose("[User] Subscription error: $error")
                        consumeEvent(UserSubscriptionEvent.ErrorOccurred(error))
                    },
                    onSubscribe = { logger.verbose("[User] Subscription established.") },
                    onRetrying = { logger.verbose("[User] Subscription lost. Trying again.") },
                    onEnd = { error -> logger.verbose("[User] Subscription ended with: $error") }
            ),
            messageParser = UserSubscriptionEventParser,
            resolveOnFirstEvent = true
    )

    override fun connect() =
            underlyingSubscription.connect()

    override fun unsubscribe() =
            underlyingSubscription.unsubscribe()
}