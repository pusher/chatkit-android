package com.pusher.chatkit.rooms

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger

internal class RoomSubscription(
        roomId: Int,
        consumeEvent: RoomSubscriptionConsumer,
        client: PlatformClient,
        messageLimit: Int,
        logger: Logger
) : ChatkitSubscription {
    private var roomSubscription = ResolvableSubscription(
            client = client,
            path = "/rooms/$roomId?&message_limit=$messageLimit",
            listeners = SubscriptionListeners(
                    onEvent = { consumeEvent(it.body) },
                    onError = { consumeEvent(RoomSubscriptionEvent.ErrorOccurred(it)) }
            ),
            messageParser = RoomSubscriptionEventParser,
            description = "Room $roomId",
            logger = logger
    )

    init {
        check(messageLimit >= 0) { "messageLimit must be positive" }
    }

    override fun connect() = roomSubscription.connect()

    override fun unsubscribe() {
        roomSubscription.unsubscribe()
    }
}