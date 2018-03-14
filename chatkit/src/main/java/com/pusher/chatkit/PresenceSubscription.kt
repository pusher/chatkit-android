package com.pusher.chatkit

import com.pusher.chatkit.User.Presence.Offline
import com.pusher.chatkit.User.Presence.Online
import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.fold
import elements.Subscription
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel

class PresenceSubscription(
    val instance: Instance,
    path: String,
    val userStore: GlobalUserStore,
    val tokenProvider: TokenProvider,
    val tokenParams: ChatkitTokenParams?,
    val logger: Logger,
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
                    val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)

                    when (chatEvent.eventName) {
                        "presence_update" -> arrayOf(ChatManager.GSON.fromJson<UserPresence>(chatEvent.data, UserPresence::class.java))
                        "join_room_presence_update" -> ChatManager.GSON.fromJson<UserPresences>(chatEvent.data, UserPresences::class.java).userStates
                        "initial_state" -> ChatManager.GSON.fromJson<UserPresences>(chatEvent.data, UserPresences::class.java).userStates
                        else -> emptyArray()
                    }.forEach { presence ->
                        userStore.findOrGetUser(presence.userId)
                            .fold(
                                onFailure = { ErrorOccurred(it) as ChatKitEvent },
                                onSuccess = { user ->
                                    when {
                                        user.online != presence.isOnline() -> UserPresenceUpdated(user, if (presence.isOnline()) Online else Offline)
                                        else -> NoEvent
                                    }
                                }
                            )
                            .onReady { event ->
                                if (event is UserPresenceUpdated) {
                                    event.user.online = presence.isOnline()
                                }
                                events.offer(event)
                            }
                    }
                },
                onError = { error ->
                    logger.debug("Something bad happened when trying to establish presence subscription $error")
                }
            )
        )
    }

    fun unsubscribe() {
        subscription.unsubscribe()
    }
}

data class UserPresence(val state: String, val lastSeenAt: String, val userId: String) {
    fun isOnline(): Boolean = state.equals(other = "online", ignoreCase = true)
}

data class UserPresences(val userStates: Array<UserPresence>)