package com.pusher.chatkit.subscription

import com.pusher.platform.SubscriptionListeners
import elements.EOSEvent
import elements.Headers
import elements.SubscriptionEvent

/**
 * Platform SubscriptionListeners, but as an interface.
 *
 * In future, consider pushing this interface down in to platform as an alternative to the
 * SubscriptionListeners data-class-full-of-closures approach.
 */
internal interface SubscriptionListener<A> {
    fun onEnd(error: EOSEvent?)
    fun onError(error: elements.Error)
    fun onEvent(event: SubscriptionEvent<A>)
    fun onOpen(headers: Headers)
    fun onRetrying()
    fun onSubscribe()

    fun asPlatformListeners() = SubscriptionListeners(
        onEnd = this::onEnd,
        onError = this::onError,
        onEvent = this::onEvent,
        onOpen = this::onOpen,
        onRetrying = this::onRetrying,
        onSubscribe = this::onSubscribe
    )
}
