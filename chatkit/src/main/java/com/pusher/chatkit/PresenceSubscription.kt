package com.pusher.chatkit

import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Subscription
import elements.SubscriptionEvent

class PresenceSubscription(
        val instance: Instance,
        path: String,
        val userStore: GlobalUserStore,
        val tokenProvider: TokenProvider,
        val tokenParams: ChatkitTokenParams?,
        val logger: Logger,
        val listeners: ThreadedUserSubscriptionListeners
){
    var subscription: Subscription
    init {
        subscription = instance.subscribeResuming(
                path = path,
                tokenParams = tokenParams,
                tokenProvider = tokenProvider,
                listeners = SubscriptionListeners(
                        onEvent = { event ->
                            val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)

                            when(chatEvent.eventName){

                                "presence_update" -> {

                                    val userPresence = ChatManager.GSON.fromJson<UserPresence>(chatEvent.data, UserPresence::class.java)
                                    userStore.findOrGetUser(
                                            id = userPresence.userId,
                                            userListener = UserListener { user ->
                                                if(userPresence.isOnline()) listeners.userCameOnline(user)
                                                else listeners.userWentOffline(user)

                                                user.online = userPresence.isOnline()
                                            },
                                            errorListener = ErrorListener {
                                                logger.warn("Failed getting user for a presence update")
                                            }
                                    )

                                }
                                "join_room_presence_update" -> {
                                    val userPresences = ChatManager.GSON.fromJson<UserPresences>(chatEvent.data, UserPresences::class.java)
                                    userPresences.userStates.forEach { userPresence ->
                                        userStore.findOrGetUser(
                                                id = userPresence.userId,
                                                userListener = UserListener { user ->
                                                    user.online = userPresence.isOnline()
                                                },
                                                errorListener = ErrorListener {
                                                    logger.warn("Failed getting user for a presence update")
                                                }
                                        )
                                    }
                                }
                                "initial_state" -> {
                                    val userPresences = ChatManager.GSON.fromJson<UserPresences>(chatEvent.data, UserPresences::class.java)
                                    userPresences.userStates.forEach { userPresence ->
                                        userStore.findOrGetUser(
                                                id = userPresence.userId,
                                                userListener = UserListener { user ->
                                                    user.online = userPresence.isOnline()
                                                },
                                                errorListener = ErrorListener {
                                                    logger.warn("Failed getting user for a presence update")
                                                }
                                        )
                                    }                                }
                            }

                        },
                        onError = { error ->
                            logger.debug("Something bad happened when trying to establish presence subscription $error")
                        }
                )
        )
    }

    fun unsubscribe(){
        subscription.unsubscribe()
    }
}

data class UserPresence(val state: String, val lastSeenAt: String, val userId: String){
    fun isOnline(): Boolean = state.equals(other = "online", ignoreCase = true)
}

data class UserPresences(val userStates: Array<UserPresence>)