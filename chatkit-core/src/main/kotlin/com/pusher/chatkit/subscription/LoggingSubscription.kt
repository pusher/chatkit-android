package com.pusher.chatkit.subscription

import com.pusher.chatkit.PlatformClient
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.loggingListeners
import com.pusher.platform.network.DataParser

internal fun <A> loggingSubscription(
    path: String,
    client: PlatformClient,
    listeners: SubscriptionListeners<A>,
    messageParser: DataParser<A>,
    logger: Logger,
    description: String
) = client.subscribe(
        path = path,
        listeners = SubscriptionListeners.compose(
                loggingListeners(description, logger),
                listeners
        ),
        messageParser = messageParser
)
