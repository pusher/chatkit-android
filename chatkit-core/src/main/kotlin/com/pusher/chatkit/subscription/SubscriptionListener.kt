package com.pusher.chatkit.subscription

import com.pusher.platform.SubscriptionListeners
import elements.EosError
import elements.SubscriptionEvent

/**
 * Interface for consuming subscription events.
 *
 * This is a slight simplification of the interface exposed by the platform library.
 */
internal interface SubscriptionListener<A> {
    fun onSubscribe()
    fun onOpen()
    fun onEvent(elementsEvent: SubscriptionEvent<A>)
    fun onError(error: elements.Error)
    fun onRetrying()
    fun onEnd(error: EosError?)

    fun asPlatformListeners() = SubscriptionListeners(
        onSubscribe = ::onSubscribe,
        onOpen = { onOpen() },
        onEvent = ::onEvent,
        onError = ::onError,
        onRetrying = ::onRetrying,
        onEnd = { eosEvent -> onEnd(eosEvent?.error) }
    )
}
