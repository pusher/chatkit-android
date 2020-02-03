package com.pusher.chatkit

import elements.Subscription

internal fun dummySubscription() =
        object : Subscription {
            override fun unsubscribe() { /* nop */ }
        }