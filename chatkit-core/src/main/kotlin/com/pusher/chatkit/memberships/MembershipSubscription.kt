package com.pusher.chatkit.memberships

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.platform.SubscriptionListeners
import elements.Subscription
import elements.SubscriptionEvent
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent.*
import java.util.concurrent.Future
import com.pusher.util.*
import elements.Errors
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.InstanceType
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.network.Wait
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.waitOr
import elements.Error
import java.util.concurrent.TimeUnit.SECONDS

internal class MembershipSubscription(
    private val roomId: Int,
    private val chatManager: ChatManager,
    private val consumeEvent: (ChatManagerEvent) -> Unit
) : ChatkitSubscription {

    private var active = false
    private val logger = chatManager.dependencies.logger
    private val roomStore = chatManager.roomService.roomStore
    private lateinit var subscription: Subscription

    override fun connect(): ChatkitSubscription {
        subscription = ResolvableSubscription(
            path = "/rooms/$roomId/memberships",
            listeners = SubscriptionListeners(
                onOpen = { headers ->
                    active = true
                    logger.verbose("[Membership] OnOpen $headers")
                },
                onEvent = { event: SubscriptionEvent<MembershipSubscriptionEvent> ->
                    event.body
                        .applySideEffects()
                        .toChatManagerEvent()
                        .waitOr(Wait.For(10, SECONDS)) { ErrorOccurred(Errors.other(it)).asSuccess() }
                        .recover { ErrorOccurred(it) }
                        .also(consumeEvent)
                        .also { logger.verbose("[Membership] Event received $event") }
                },
                onError = { error -> consumeEvent(ChatManagerEvent.ErrorOccurred(error)) },
                onSubscribe = { logger.verbose("[Membership] Subscription established") },
                onRetrying = { logger.verbose("[Membership] Subscription lost. Trying again.") },
                onEnd = { error -> logger.verbose("[Membership] Subscription ended with: $error") }
            ),
            messageParser = MembershipSubscriptionEventParser,
            chatManager = chatManager,
            resolveOnFirstEvent = true
        ).connect()

        return this
    }

    override fun unsubscribe() {
        active = false
        subscription.unsubscribe()
    }

    private fun MembershipSubscriptionEvent.applySideEffects(): MembershipSubscriptionEvent = this.apply {
        when (this) {
            is InitialState -> {
                userIds.forEach { userId -> roomStore[roomId]?.addUser(userId) }
            }
            is UserJoined -> {
                roomStore[roomId]?.addUser(userId)
            }
            is UserLeft -> {
                roomStore[roomId]?.removeUser(userId)
            }
        }
    }


    private fun MembershipSubscriptionEvent.toChatManagerEvent(): Future<Result<ChatManagerEvent, Error>> = when (this) {
        is InitialState -> {
            NoEvent.asSuccess<ChatManagerEvent, Error>().toFuture()
        }
        is UserLeft -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserLeftRoom(user, room) }
        }
        is UserJoined -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserJoinedRoom(user, room) }
        }
    }
}
