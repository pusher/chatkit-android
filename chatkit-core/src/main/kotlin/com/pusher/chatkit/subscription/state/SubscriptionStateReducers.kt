package com.pusher.chatkit.subscription.state

import com.pusher.chatkit.state.SubscriptionStateAction
import com.pusher.chatkit.state.subscriptionStateReducer

internal val subscriptionInitializingReducer =
    subscriptionStateReducer<SubscriptionStateAction.Initializing> { state, action ->
        when (action.subscriptionId) {
            is SubscriptionId.UserSubscription -> state.with(
                userSubscription = SubscriptionStatus.Connecting
            )
        }
    }

internal val subscriptionOpenReducer =
    subscriptionStateReducer<SubscriptionStateAction.Open> { state, action ->
        when (action.subscriptionId) {
            is SubscriptionId.UserSubscription -> state.with(
                userSubscription = SubscriptionStatus.Connected
            )
        }
    }

internal val subscriptionErrorReducer =
    subscriptionStateReducer<SubscriptionStateAction.Error> { state, action ->
        when (action.subscriptionId) {
            is SubscriptionId.UserSubscription -> state.with(
                userSubscription = SubscriptionStatus.Reconnecting(
                    action.error
                )
            )
        }
    }

internal val subscriptionEndReducer =
    subscriptionStateReducer<SubscriptionStateAction.End> { state, action ->
        when (action.subscriptionId) {
            is SubscriptionId.UserSubscription -> state.with(
                userSubscription = SubscriptionStatus.Closed(
                    action.error
                )
            )
        }
    }
