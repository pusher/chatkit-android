package com.pusher.chatkit

import com.pusher.chatkit.User.Presence.Offline
import com.pusher.chatkit.User.Presence.Online
import com.pusher.chatkit.network.parseAs
import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.map
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.wait
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.asFailure
import com.pusher.util.mapResult
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

class PresenceSubscription(
    val instance: Instance,
    path: String,
    val tokenProvider: TokenProvider,
    val tokenParams: ChatkitTokenParams?,
    private val chatManager: ChatManager,
    private val consumeEvent: (ChatManagerEvent) -> Unit
) : Subscription {

    private val subscription: Subscription = instance.subscribeResuming<ChatEvent>(
        path = path,
        tokenParams = tokenParams,
        tokenProvider = tokenProvider,
        listeners = SubscriptionListeners(
            onEvent = { event ->
                event.body
                    .toUserPreferences()
                    .map { presences: List<UserPresence> -> presences.map { eventForPresence(it.userId, it) } }
                    .recover { error -> listOf((ErrorOccurred(error) as ChatManagerEvent).toFuture()) }
                    .forEach { consumeEvent(it.wait()) }
            },
            onError = { error -> consumeEvent(ErrorOccurred(error)) }
        ),
        bodyParser = { it.parseAs() }
    )

    private fun ChatEvent.toUserPreferences() = when (eventName) {
        "presence_update" -> data.parseAs<UserPresence>().map { listOf(it) }
        "initial_state", "join_room_presence_update" -> data.parseAs<UserPresences>().map { it.userStates }
        else -> Errors.network("Not a valid eventName for ChatEvent: $eventName").asFailure()
    }

    private fun eventForPresence(userId: String, presence: UserPresence): Future<ChatManagerEvent> =
        chatManager.userService().fetchUserBy(userId)
            .mapResult { user ->
                when {
                    user.online != presence.isOnline() -> {
                        user.online = presence.isOnline() // TODO: solve mutability
                        UserPresenceUpdated(user, presence.toUserPresence())
                    }
                    else -> NoEvent
                }
            }
            .map { it.recover { error -> ErrorOccurred(error) } }


    private fun UserPresence.toUserPresence(): User.Presence =
        if (isOnline()) Online else Offline

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}

data class UserPresence(val state: String, val lastSeenAt: String, val userId: String) {
    fun isOnline(): Boolean = state.equals(other = "online", ignoreCase = true)
}

data class UserPresences(val userStates: List<UserPresence>)
