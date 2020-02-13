package com.pusher.chatkit.cursors

import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.dummySubscription
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.platform.SubscriptionListeners
import elements.emptyHeaders
import org.mockito.ArgumentMatchers.startsWith

internal fun justConnectingCursorSubscription():
        KStubbing<PlatformClient>.(PlatformClient) -> Unit = { client ->
    on {
        client.subscribeResuming(
                path = startsWith("/cursors/0/rooms/"), // read type (0) cursors for any room
                listeners = any(),
                messageParser = any<CursorSubscriptionEventParser>()
        )
    } doAnswer { invocation ->

        @Suppress("UNCHECKED_CAST")
        val listener = invocation.arguments[1] as SubscriptionListeners<UserSubscriptionEvent>
        listener.onOpen(emptyHeaders())

        dummySubscription()
    }
}
