package com.pusher.chatkit.presence

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.map
import com.pusher.util.Result
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Subscription
import elements.SubscriptionEvent
import java.util.concurrent.Future

internal class PresenceSubscription(
    private val client: PlatformClient,
    private val userId: String,
    private val consumeEvent: (PresenceSubscriptionEvent) -> Unit,
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
                    consumeEvent(event.toPresenceEvent().recover { PresenceSubscriptionEvent.ErrorOccurred(it) })
                },
                onError = { error -> consumeEvent(PresenceSubscriptionEvent.ErrorOccurred(error)) },
                onEnd = { error -> logger.verbose("[Presence] Subscription ended with: $error") }
            ),
            messageParser = { it.parseAs() },
            resolveOnFirstEvent = true
        ).connect()

        return this
    }

    private fun SubscriptionEvent<ChatEvent>.toPresenceEvent(): Result<PresenceSubscriptionEvent, Error> = when (body.eventName) {
        "presence_update" -> body.data.parseAs<UserPresence>().map { PresenceSubscriptionEvent.PresenceUpdate(it) }
        "initial_state" -> body.data.parseAs<UserPresences>().map { PresenceSubscriptionEvent.InitialState(it.userStates) }
        "join_room_presence_update" -> body.data.parseAs<UserPresences>().map { PresenceSubscriptionEvent.JoinedRoom(it.userStates) }
        else -> PresenceSubscriptionEvent.NoEvent.asSuccess()
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

private data class UserPresences(val userStates: List<UserPresence>)
