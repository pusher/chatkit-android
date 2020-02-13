package com.pusher.chatkit

import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.users.UserSubscriptionEventParser
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.Futures
import elements.Error
import elements.SubscriptionEvent
import elements.emptyHeaders

internal fun mockPlatformClientForUserSubscription(
    vararg events: UserSubscriptionEvent,
    error: Error? = null
): PlatformClient = mockPlatformClient(userSubscription(*events, error = error))

internal fun userSubscription(vararg events: UserSubscriptionEvent, error: Error? = null):
        KStubbing<PlatformClient>.(PlatformClient) -> Unit = { client ->
    on {
        client.subscribeResuming(
                path = eq("users"),
                listeners = any(),
                messageParser = any<UserSubscriptionEventParser>()
        )
    } doAnswer { invocation ->

        @Suppress("UNCHECKED_CAST")
        val listener = invocation.arguments[1] as SubscriptionListeners<UserSubscriptionEvent>

        Futures.schedule {
            for (event in events) {
                val eventId = mapToSubscriptionEventId(event)
                listener.onEvent(SubscriptionEvent(eventId, emptyHeaders(), event))
            }

            if (error != null) listener.onError(error)
        }

        dummySubscription()
    }
}

private fun mapToSubscriptionEventId(event: UserSubscriptionEvent): String {
    return when (event) {
        is UserSubscriptionEvent.InitialState -> "initial_state"
        is UserSubscriptionEvent.AddedToRoomEvent -> "added_to_room"
        is UserSubscriptionEvent.RemovedFromRoomEvent -> "removed_from_room"
        is UserSubscriptionEvent.RoomUpdatedEvent -> "room_updated"
        is UserSubscriptionEvent.RoomDeletedEvent -> "room_deleted"
        is UserSubscriptionEvent.ReadStateUpdatedEvent -> "read_state_updated"
        is UserSubscriptionEvent.UserJoinedRoomEvent -> "user_joined_room"
        is UserSubscriptionEvent.UserLeftRoomEvent -> "user_left_room"
        is UserSubscriptionEvent.ErrorOccurred -> throw RuntimeException("zombie code")
    }
}
