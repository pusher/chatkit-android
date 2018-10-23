package com.pusher.chatkit.presence

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.loggingSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import elements.Subscription
import java.net.URLEncoder
import java.util.*

class PresenceService(
        myUserId: String,
        private val client: PlatformClient,
        private val consumer: PresenceSubscriptionConsumer,
        private val logger: Logger
) {
    private val subscriptions = HashMap<String, Subscription>()

    private val registrationSub =
            loggingSubscription(
                    path = "/users/${URLEncoder.encode(myUserId, "UTF-8")}/register",
                    listeners = SubscriptionListeners(),
                    messageParser = PresenceSubscriptionEventParser(myUserId),
                    logger = logger,
                    client = client,
                    description = "PresenceRegistration $myUserId"
            )

    fun subscribeToUser(userId: String) {
        synchronized(subscriptions) {
            if (!subscriptions.contains(userId)) {
                subscriptions[userId] = loggingSubscription(
                        path = "/users/$userId",
                        listeners = SubscriptionListeners(
                                onEvent = { consumer.invoke(it.body) }
                        ),
                        messageParser = PresenceSubscriptionEventParser(userId),
                        logger = logger,
                        client = client,
                        description = "Presence $userId"
                )
            }
        }
    }

    fun unsubscribeFromUser(userId: String) {
        synchronized(subscriptions) {
            val subscription = subscriptions[userId]
            if (subscription != null) {
                subscription.unsubscribe()
                subscriptions.remove(userId)
            }
        }
    }

    fun close() {
        synchronized(subscriptions) {
            for (subscription in subscriptions.values) {
                subscription.unsubscribe()
            }
            subscriptions.clear()
        }
        registrationSub.unsubscribe()
    }
}