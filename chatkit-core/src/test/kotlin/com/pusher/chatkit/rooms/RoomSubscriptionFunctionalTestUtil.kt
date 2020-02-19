package com.pusher.chatkit.rooms

import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.dummySubscription
import com.pusher.chatkit.rooms.api.RoomSubscriptionEvent
import com.pusher.chatkit.users.api.UserSubscriptionEvent
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.DataParser
import elements.emptyHeaders
import org.mockito.ArgumentMatchers.startsWith

internal fun justConnectingRoomSubscription():
        KStubbing<PlatformClient>.(PlatformClient) -> Unit = { client ->
    on {
        client.subscribeResuming(
                path = startsWith("/rooms/"),
                listeners = any(),
                messageParser = any<DataParser<RoomSubscriptionEvent>>()
        )
    } doAnswer { invocation ->

        @Suppress("UNCHECKED_CAST")
        val listener = invocation.arguments[1] as SubscriptionListeners<UserSubscriptionEvent>
        listener.onOpen(emptyHeaders())

        dummySubscription()
    }
}
