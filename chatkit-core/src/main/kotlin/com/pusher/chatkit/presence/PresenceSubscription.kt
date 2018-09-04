package com.pusher.chatkit.presence

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.ChatManagerEventConsumer
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.map
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.wait
import com.pusher.util.asFailure
import com.pusher.util.mapResult
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal class PresenceSubscription(
    private val client: PlatformClient,
    private val userId: String,
    private val consumeEvent: ChatManagerEventConsumer,
    private val userService: UserService,
    private val logger: Logger
): ChatkitSubscription {
    private var active = false
    private lateinit var subscription: Subscription

    override fun connect(): ChatkitSubscription {
        subscription = ResolvableSubscription(
            client = client,
            path = "/users/$userId/presence",
            listeners = SubscriptionListeners<ChatEvent>(
                onOpen = { headers ->
                    logger.verbose("[Presence] OnOpen $headers")
                    active = true
                },
                onEvent = { event ->
                    val presenceEventFutures = event.body
                        .toUserPresences()
                        .map { presences: List<UserPresence> -> presences.map { eventForPresence(it.userId, it.presence) } }
                        .recover { error -> listOf((ChatManagerEvent.ErrorOccurred(error) as ChatManagerEvent).toFuture()) }

                    for (presEventFuture in presenceEventFutures) {
                        consumeEvent(presEventFuture.wait())
                    }
                },
                onError = { error -> consumeEvent(ChatManagerEvent.ErrorOccurred(error)) },
                onEnd = { error -> logger.verbose("[Presence] Subscription ended with: $error") }
            ),
            messageParser = { it.parseAs() },
            resolveOnFirstEvent = true
        ).connect()

        return this
    }

    private fun ChatEvent.toUserPresences() = when (eventName) {
        "presence_update" -> data.parseAs<UserPresence>().map { listOf(it) }
        "initial_state", "join_room_presence_update" -> data.parseAs<UserPresences>().map { it.userStates }
        else -> Errors.network("Not a valid eventName for ChatEvent: $eventName").asFailure()
    }

    private fun eventForPresence(userId: String, presence: Presence): Future<ChatManagerEvent> =
        userService.fetchUserBy(userId)
            .mapResult { user ->
                user.takeIf { it.presence != presence }
                    ?.also { it.presence = presence }
                    ?.presence
                    .let { userPresence ->
                        when (userPresence) {
                            Presence.Online -> ChatManagerEvent.UserCameOnline(user)
                            Presence.Offline -> ChatManagerEvent.UserWentOffline(user)
                            null -> ChatManagerEvent.NoEvent
                        }
                    }
            }
            .map { it.recover { error -> ChatManagerEvent.ErrorOccurred(error) } }

    override fun unsubscribe() {
        active = false
        subscription.unsubscribe()
    }
}

private data class UserPresence(
    private val state: String,
    val lastSeenAt: String,
    val userId: String
) {
    val presence: Presence
        get() = when(state) {
            "online" -> Presence.Online
            else -> Presence.Offline
        }

}

private data class UserPresences(val userStates: List<UserPresence>)
