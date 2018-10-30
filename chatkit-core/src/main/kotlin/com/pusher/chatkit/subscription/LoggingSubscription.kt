package com.pusher.chatkit.subscription

import com.pusher.chatkit.PlatformClient
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.loggingListeners
import com.pusher.platform.network.DataParser

// Implements a subscription that can be used to resolve subscriptions
// either on opening a subscription (default behaviour) or
// on receiving the first event on the subscription
fun <A>loggingSubscription(
        path: String,
        client: PlatformClient,
        listeners: SubscriptionListeners<A>,
        messageParser: DataParser<A>,
        logger: Logger,
        description: String
) = client.subscribeResuming(
        path = path,
        listeners = SubscriptionListeners.compose(
                loggingListeners(description, logger),
                listeners
        ),
        messageParser = messageParser
)
