package com.pusher.chatkit.subscription

import com.pusher.platform.SubscriptionListeners
import elements.EosError
import elements.Headers
import elements.SubscriptionEvent

/**
 * Platform SubscriptionListeners, but as an interface.
 *
 * In future, consider pushing this interface down in to platform as an alternative to the
 * SubscriptionListeners data-class-full-of-closures approach.
 */
internal interface SubscriptionListener<A> {
    fun onEnd(error: EosError?)
    fun onError(error: elements.Error)
    fun onEvent(elementsEvent: SubscriptionEvent<A>)
    fun onOpen()
    fun onRetrying()
    fun onSubscribe()

    fun asPlatformListeners() = SubscriptionListeners(
        onEnd = { eosEvent -> onEnd(eosEvent?.error) },
        onError = ::onError,
        onEvent = ::onEvent,
        onOpen = { onOpen() },
        onRetrying = ::onRetrying,
        onSubscribe = ::onSubscribe
    )
}
