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
                    onEvent = { event -> consumeEvent(event.body) },
                    onError = { error -> consumeEvent(MembershipSubscriptionEvent.ErrorOccurred(error)) }
            ),
            messageParser = MembershipSubscriptionEventParser,
            logger = logger,
            description = "Memberships room $roomId",
            resolveOnFirstEvent = true
    )

    override fun connect(): ChatkitSubscription {
        return subscription.connect()
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}