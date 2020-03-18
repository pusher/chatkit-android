package com.pusher.chatkit.state

import com.pusher.chatkit.subscription.state.SubscriptionStatus

internal data class SubscriptionState(
    val userSubscription: SubscriptionStatus
) {
    companion object {
        fun initial() = SubscriptionState(
            userSubscription = SubscriptionStatus.Initial
        )
    }

    fun with(userSubscription: SubscriptionStatus) = copy(
        userSubscription = userSubscription
    )
}
