package com.pusher.chatkit

import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import elements.EOSEvent
import elements.Subscription
import elements.SubscriptionEvent

class UserSubscription(
        instance: Instance,
        path: String,
        val userStore: GlobalUserStore,
        val logger: Logger,
        onCurrentUser: CurrentUserListener,
        onError: ErrorListener
) {

    var subscription: Subscription? = null

    init {
        subscription = instance.subscribeResuming(
                path = path,
                listeners = SubscriptionListeners(
                        onOpen = { headers ->
                            logger.warn("OnOpen $headers")
                        },
                        onEvent = { event ->

                            logger.warn("Event $event")

                            handleEvent(event) },
                        onError = { error ->
                            logger.warn("Error $error")
                            onError.onError(error) },
                        onSubscribe = {
                            logger.warn("onSubscribe")
                        },
                        onRetrying = {
                            logger.warn("onRetrying")
                        },
                        onEnd = {
                            error ->
                            logger.warn("onEnd $error")

                        }
                )
        )

        logger.warn("User subscription JEBOTELED")
    }

    fun handleEvent(event: SubscriptionEvent) {

        logger.warn("Handle event: $event")

        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)
        when(chatEvent.eventName){
            EventType.INITIAL_STATE -> {
                val body = ChatManager.GSON.fromJson<InitialState>(chatEvent.data, InitialState::class.java)
                handleInitialState(body)
            }

            else -> { }
        }
    }

    private fun handleInitialState(initialState: InitialState) {
        logger.warn("Initial state received $initialState")

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}