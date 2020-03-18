package com.pusher.chatkit.users.api

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.SubscriptionListener
import com.pusher.chatkit.subscription.loggingSubscription
import com.pusher.platform.logger.Logger
import org.koin.core.module.Module
import org.koin.dsl.module

internal typealias UserSubscriptionListener = SubscriptionListener<UserSubscriptionEvent>

internal val createUserSubscriberModule: () -> Module = {
    module {
        factory { UserSubscriberFactory(get(), get()) }
    }
}

internal class UserSubscriberFactory(
    private val client: PlatformClient,
    private val logger: Logger
) {

    fun create(listener: UserSubscriptionListener) = UserSubscriber(client, logger, listener)
}

internal class UserSubscriber(
    private val client: PlatformClient,
    private val logger: Logger,
    private val listener: UserSubscriptionListener
) {

    fun subscribe() = loggingSubscription(
        client = client,
        path = "users",
        listeners = listener.asPlatformListeners(),
        messageParser = UserSubscriptionEventParser,
        description = "User",
        logger = logger
    )
}
