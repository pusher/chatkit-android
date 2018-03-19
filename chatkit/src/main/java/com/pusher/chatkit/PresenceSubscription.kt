package com.pusher.chatkit

import com.pusher.chatkit.User.Presence.Offline
import com.pusher.chatkit.User.Presence.Online
import com.pusher.chatkit.network.parseAs
import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.asFailure
import com.pusher.util.fold
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
    private val events: BroadcastChannel<ChatKitEvent> = BroadcastChannel(Channel.CONFLATED)
) : BroadcastChannel<ChatKitEvent> by events {

    var subscription: Subscription

    init {
        subscription = instance.subscribeResuming(
            path = path,
            tokenParams = tokenParams,
            tokenProvider = tokenProvider,
            listeners = SubscriptionListeners(
                onEvent = { event ->
                    event.body.parseAs<ChatEvent>()
                        .flatMap { (eventName, _, _, data) ->
                            when (eventName) {
                                "presence_update" -> data.parseAs<UserPresence>().map { arrayOf(it) }
                                "join_room_presence_update" -> data.parseAs<UserPresences>().map { it.userStates }
                                "initial_state" -> data.parseAs<UserPresences>().map { it.userStates }
                                else -> Errors.network("Not a valid eventName for ChatEvent: $eventName").asFailure()
                            }
                        }
                        .fold(
                            { error -> events.offer(ErrorOccurred(error)) },
                            { presences -> presences.forEach { updatePresenceForUser(it.userId, it) } }
                        )
                },
                onError = { error -> events.offer(ErrorOccurred(error)) }
            )
        )
    }

    private fun updatePresenceForUser(userId: String, presence: UserPresence) {
        chatManager.userService().fetchUserBy(userId)
            .fold({ error ->
                ErrorOccurred(error) as ChatKitEvent
            }, { user ->
                when {
                    user.online != presence.isOnline() -> UserPresenceUpdated(user, if (presence.isOnline()) Online else Offline)
                    else -> NoEvent
                }
            })
            .onReady { event ->
                if (event is UserPresenceUpdated) {
                    event.user.online = presence.isOnline()
                }
                events.offer(event)
            }
    }

    fun unsubscribe() {
        subscription.unsubscribe()
    }
}

data class UserPresence(val state: String, val lastSeenAt: String, val userId: String) {
    fun isOnline(): Boolean = state.equals(other = "online", ignoreCase = true)
}

data class UserPresences(val userStates: Array<UserPresence>)
