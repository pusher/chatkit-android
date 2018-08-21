package com.pusher.chatkit.subscription

import elements.Subscription

interface ChatkitSubscription: Subscription {
    fun connect(): ChatkitSubscription
}