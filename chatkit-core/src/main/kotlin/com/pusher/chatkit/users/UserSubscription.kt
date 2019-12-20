package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.loggingSubscription
import com.pusher.chatkit.util.FutureValue
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Subscription
import java.util.concurrent.atomic.AtomicBoolean

internal class UserSubscription(
        userId: String,
        client: PlatformClient,
        logger: Logger,
        private val listeners: UserSubscriptionConsumer
) : Subscription {

    private val initialized = AtomicBoolean(false)

    private val initialState: FutureValue<Result<UserSubscriptionEvent.InitialState, Error>> = FutureValue()

    private val subscription = loggingSubscription(
            client = client,
            path = "users",
            listeners = SubscriptionListeners(
                    onEvent = { event -> consumeEvent(event.body) },
                    onError = { error -> consumeEvent(UserSubscriptionEvent.ErrorOccurred(error)) }
            ),
            messageParser = UserSubscriptionEventParser,
            description = "User $userId",
            logger = logger
    )

    private fun consumeEvent(event: UserSubscriptionEvent) {
        if (event is UserSubscriptionEvent.InitialState
                && !initialized.getAndSet(true)) {
            initialState.set(event.asSuccess())
        } else if (event is UserSubscriptionEvent.ErrorOccurred
                && !initialized.getAndSet(true)) {
            initialState.set(event.error.asFailure())
        } else {
            listeners.invoke(event)
        }
    }

    fun initialState(): Result<UserSubscriptionEvent.InitialState, Error> {
        return initialState.get()
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
    }
}