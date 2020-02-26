package com.pusher.chatkit.users.api

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.loggingSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import org.koin.core.module.Module
import org.koin.dsl.module

internal val createUserSubscriberModule: () -> Module = {
    module {
        factory { UserSubscriberFactory(get(), get()) }
    }
}

internal class UserSubscriberFactory(
    private val client: PlatformClient,
    private val logger: Logger
) {

    fun create(listeners: UserSubscriptionConsumer) = UserSubscriber(client, logger, listeners)
}

internal class UserSubscriber(
    private val client: PlatformClient,
    private val logger: Logger,
    private val listeners: UserSubscriptionConsumer
) {

    fun subscribe() = loggingSubscription(
        client = client,
        path = "users",
        listeners = SubscriptionListeners(
            onEvent = { event -> consumeEvent(event.body) },
            onError = { error -> consumeEvent(UserSubscriptionEvent.ErrorOccurred(error)) }
            // TODO: handle onEnd, e.g. when tackling connection status (terminal/non-terminal errs)
        ),
        messageParser = UserSubscriptionEventParser,
        description = "User",
        logger = logger
    )

    private fun consumeEvent(event: UserSubscriptionEvent) {
        listeners.invoke(event)
    }
}
