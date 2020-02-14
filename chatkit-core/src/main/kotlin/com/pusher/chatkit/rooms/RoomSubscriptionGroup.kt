package com.pusher.chatkit.rooms

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.DataParser
import elements.Subscription
import java.net.URLEncoder

internal class RoomSubscriptionGroup(
    messageLimit: Int,
    roomId: String,
    cursorService: CursorService,
//    cursorConsumer: ChatManagerEventConsumer,
    roomConsumer: RoomSubscriptionConsumer,
    client: PlatformClient,
    messageParser: DataParser<RoomSubscriptionEvent>,
    logger: Logger
) : ChatkitSubscription {
    init {
        check(messageLimit >= 0) { "messageLimit must be greater than or equal to 0" }
    }

    private val roomSubscription = ResolvableSubscription(
            client = client,
            path = "/rooms/${URLEncoder.encode(roomId, "UTF-8")}?&message_limit=$messageLimit",
            listeners = SubscriptionListeners(
                    onEvent = { roomConsumer(it.body) },
                    onError = { roomConsumer(RoomSubscriptionEvent.ErrorOccurred(it)) }
            ),
            messageParser = messageParser,
            description = "Room $roomId",
            logger = logger
    )

    private val cursorsSubscription = cursorService.subscribeForRoom(
            roomId
//            consumer = cursorConsumer
    )

    override fun connect(): Subscription {
        roomSubscription.await()
//        cursorsSubscription.await()

        return this
    }

    override fun unsubscribe() {
        roomSubscription.unsubscribe()
//        cursorsSubscription.unsubscribe()
    }
}
