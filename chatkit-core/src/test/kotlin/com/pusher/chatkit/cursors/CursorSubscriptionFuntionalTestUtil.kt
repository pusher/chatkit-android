package com.pusher.chatkit.cursors

import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.dummySubscription
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.platform.SubscriptionListeners
import elements.emptyHeaders
import org.mockito.ArgumentMatchers

internal fun justConnectingCursorSubscription()
        : KStubbing<PlatformClient>.(PlatformClient) -> Unit = { client ->
    on {
        client.subscribeResuming(
                ArgumentMatchers.startsWith("/cursors/0/rooms/"),
                any(),
                any<CursorSubscriptionEventParser>()
        )
    } doAnswer { invocation ->

        @Suppress("UNCHECKED_CAST")
        val listener = invocation.arguments[1] as SubscriptionListeners<UserSubscriptionEvent>
        listener.onOpen(emptyHeaders())

        dummySubscription()
    }
}