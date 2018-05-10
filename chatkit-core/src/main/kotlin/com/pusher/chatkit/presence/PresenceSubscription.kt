package com.pusher.chatkit.presence

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.ChatManagerEventConsumer
import com.pusher.chatkit.network.parseAs
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.map
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.wait
import com.pusher.util.asFailure
import com.pusher.util.mapResult
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal class PresenceSubscription(
    chatManager: ChatManager,
    userId: String,
    private val consumeEvent: ChatManagerEventConsumer
) : Subscription {

    private val userService = chatManager.userService

    private val subscription: Subscription = chatManager.presenceInstance.subscribeResuming<ChatEvent>(
        path = "/users/$userId/presence",
        tokenParams = chatManager.dependencies.tokenParams,
        tokenProvider = chatManager.tokenProvider,
        listeners = SubscriptionListeners(
            onEvent = { event ->
                event.body
                    .toUserPresences()
                    .map { presences: List<UserPresence> -> presences.map { eventForPresence(it.userId, it.presence) } }
                    .recover { error -> listOf((ErrorOccurred(error) as ChatManagerEvent).toFuture()) }
                    .forEach { consumeEvent(it.wait()) }
            },
            onError = { error -> consumeEvent(ErrorOccurred(error)) }
        ),
        messageParser = { it.parseAs() }
    )

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
                            Presence.Online -> UserCameOnline(user)
                            Presence.Offline -> UserWentOffline(user)
                            null -> NoEvent
                        }
                    }
            }
            .map { it.recover { error -> ErrorOccurred(error) } }


    override fun unsubscribe() {
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
