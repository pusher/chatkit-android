package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.ChatManagerEventConsumer
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionConsumer
import com.pusher.chatkit.memberships.MembershipSubscriptionConsumer
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.memberships.MembershipSubscriptionEventParser
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.User
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import elements.Subscription
import java.net.URLEncoder
import java.util.concurrent.Future


internal class RoomSubscriptionGroup(
        messageLimit: Int,
        roomId: String,
        cursorService: CursorService,
        cursorConsumer: ChatManagerEventConsumer,
        membershipConsumer: MembershipSubscriptionConsumer,
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

    private val membershipSubscription = ResolvableSubscription(
            resolveOnFirstEvent = true,
            client = client,
            path = "/rooms/${URLEncoder.encode(roomId, "UTF-8")}/memberships",
            listeners = SubscriptionListeners(
                    onEvent = { membershipConsumer(it.body) },
                    onError = { membershipConsumer(MembershipSubscriptionEvent.ErrorOccurred(it)) }
            ),
            messageParser = MembershipSubscriptionEventParser,
            description = "Memberships room $roomId",
            logger = logger
    )

    private val cursorsSubscription = cursorService.subscribeForRoom(
            roomId,
            consumer = cursorConsumer
    )

    override fun connect(): Subscription {
        roomSubscription.await()
        membershipSubscription.await()
        cursorsSubscription.await()

        return this
    }

    override fun unsubscribe() {
        roomSubscription.unsubscribe()
        membershipSubscription.unsubscribe()
        cursorsSubscription.unsubscribe()
    }
}