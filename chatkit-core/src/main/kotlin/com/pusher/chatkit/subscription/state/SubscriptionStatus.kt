package com.pusher.chatkit.subscription.state

import elements.Error

internal sealed class SubscriptionStatus {
    /**
     * Not yet constructed
     */
    object Initial : SubscriptionStatus()

    /**
     * Has had onSubscribing called, but not either
     * - onOpen (for resumable subscription)
     * - onEvent with `initial_state` (for non-resumable subscription)
     */
    object Connecting : SubscriptionStatus()

    /**
     * Open and happy
     */
    object Connected : SubscriptionStatus()

    /**
     * Currently retrying, with last error encountered which triggered retry
     */
    data class Reconnecting(val error: Error) : SubscriptionStatus()

    /**
     * Closed, either
     * - on client request (error == null)
     * - due to terminal error (error != null)
     */
    data class Closed(val error: Error?) : SubscriptionStatus()
}
