package com.pusher.chatkit

import com.pusher.chatkit.User.Presence.Offline
import com.pusher.chatkit.User.Presence.Online
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.typeToken
import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.map
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Errors
import elements.Subscription
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel

class PresenceSubscription(
    val instance: Instance,
    path: String,
    val tokenProvider: TokenProvider,
    val tokenParams: ChatkitTokenParams?,
    private val chatManager: ChatManager,
    private val events: BroadcastChannel<ChatManagerEvent> = BroadcastChannel(Channel.CONFLATED)
) : BroadcastChannel<ChatManagerEvent> by events {

    var subscription: Subscription

    init {
        subscription = instance.subscribeResuming<ChatEvent>(
            path = path,
            tokenParams = tokenParams,
            tokenProvider = tokenProvider,
            listeners = SubscriptionListeners(
                onEvent = { event ->
                    event.body.let { (eventName, _, _, data) ->
                        when (eventName) {
                            "presence_update" -> data.parseAs<UserPresence>().map { arrayOf(it) }
                            "join_room_presence_update" -> data.parseAs<UserPresences>().map { it.userStates }
                            "initial_state" -> data.parseAs<UserPresences>().map { it.userStates }
                            else -> Errors.network("Not a valid eventName for ChatEvent: $eventName").asFailure()
                        }
                    }.fold(
                        { error -> events.offer(ErrorOccurred(error)) },
                        { presences ->
                            presences.asSequence()
                                .map { eventForPresence(it.userId, it) }
                                .map { it.get() }
                                .filter { it !== NoEvent }
                                .forEach { events.offer(it) }
                        }
                    )
                },
                onError = { error -> events.offer(ErrorOccurred(error)) }
            ),
            typeResolver = { typeToken<ChatEvent>().asSuccess() }
        )
    }

    private fun eventForPresence(userId: String, presence: UserPresence) =
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

    fun unsubscribe() {
        subscription.unsubscribe()
    }
}

data class UserPresence(val state: String, val lastSeenAt: String, val userId: String) {
    fun isOnline(): Boolean = state.equals(other = "online", ignoreCase = true)
}

data class UserPresences(val userStates: Array<UserPresence>)
