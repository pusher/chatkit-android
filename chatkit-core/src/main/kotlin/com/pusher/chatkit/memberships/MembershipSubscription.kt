package com.pusher.chatkit.memberships

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger

internal class MembershipSubscription(
    roomId: Int,
    client: PlatformClient,
    consumeEvent: MembershipSubscriptionConsumer,
    logger: Logger
) : ChatkitSubscription {
    private var subscription = ResolvableSubscription(
            client = client,
            path = "/rooms/$roomId/memberships",
            listeners = SubscriptionListeners(
                    onOpen = { headers ->
                        logger.verbose("[Membership] OnOpen $headers")
                    },
                    onEvent = { event ->
                        event.body
                                .also(consumeEvent)
                                .also { logger.verbose("[Membership] Event received $event") }
                    },
                    onError = { error -> consumeEvent(MembershipSubscriptionEvent.ErrorOccurred(error)) },
                    onSubscribe = { logger.verbose("[Membership] Subscription established") },
                    onRetrying = { logger.verbose("[Membership] Subscription lost. Trying again.") },
                    onEnd = { error -> logger.verbose("[Membership] Subscription ended with: $error") }
            ),
            messageParser = MembershipSubscriptionEventParser,
            resolveOnFirstEvent = true
    )

    override fun connect(): ChatkitSubscription {
        return subscription.connect()
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}