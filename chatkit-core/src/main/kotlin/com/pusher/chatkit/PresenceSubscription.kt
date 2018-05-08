package com.pusher.chatkit

import com.pusher.chatkit.ChatManagerEvent.*
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
                    .toUserPresences()
                    .map { presences: List<UserPresence> -> presences.map { eventForPresence(it.userId, it) } }
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

    private fun eventForPresence(userId: String, presence: UserPresence): Future<ChatManagerEvent> =
        chatManager.userService().fetchUserBy(userId)
            .mapResult { user ->
                user.takeIf { it.online != presence.isOnline() }
                    ?.also { it.online = presence.isOnline() }
                    ?.let { if (it.online) UserCameOnline(it) else UserWentOffline(it) }
                    ?: NoEvent
            }
            .map { it.recover { error -> ErrorOccurred(error) } }


    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}

data class UserPresence(val state: String, val lastSeenAt: String, val userId: String) {
    fun isOnline(): Boolean = state.equals(other = "online", ignoreCase = true)
}

data class UserPresences(val userStates: List<UserPresence>)
