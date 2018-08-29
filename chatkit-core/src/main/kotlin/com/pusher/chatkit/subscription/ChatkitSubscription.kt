package com.pusher.chatkit.subscription

import elements.Subscription

interface ChatkitSubscription: Subscription {
    // Connect must be called for the subscription to be
    // established. Creating a ChatkitSubscription instance
    // will just return an instance and do nothing i.e
    // no events will be received
    suspend fun connect(): ChatkitSubscription
}