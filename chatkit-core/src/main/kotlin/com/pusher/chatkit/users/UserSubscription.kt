package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger


internal class UserSubscription(
    client: PlatformClient,
    consumeEvent: UserSubscriptionConsumer,
    logger: Logger
) : ChatkitSubscription {
    private var underlyingSubscription = ResolvableSubscription(
            client = client,
            path = "users",
            listeners = SubscriptionListeners(
                    onEvent = { event -> consumeEvent(event.body) },
                    onError = { error -> consumeEvent(UserSubscriptionEvent.ErrorOccurred(error)) }
            ),
            messageParser = UserSubscriptionEventParser,
            resolveOnFirstEvent = true,
            description = "User",
            logger = logger
    )

    override fun connect() =
            underlyingSubscription.connect()

    override fun unsubscribe() =
            underlyingSubscription.unsubscribe()
}