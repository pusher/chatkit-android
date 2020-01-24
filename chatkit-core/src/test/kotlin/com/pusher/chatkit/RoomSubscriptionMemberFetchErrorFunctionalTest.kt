package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.cursors.justConnectingCursorSubscription
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.rooms.justConnectingRoomSubscription
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.users.usersFetchFailingWith
import com.pusher.chatkit.util.FutureValue
import elements.Errors
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object RoomSubscriptionMemberFetchErrorFunctionalTest : Spek({

    val initialState = UserSubscriptionEvent.InitialState(
            simpleUser("alice"),
            listOf(simpleRoom("roomId1", "Room 1")),
            listOf(RoomReadStateApiType("roomId1", 0, null)),
            listOf(RoomMembershipApiType("roomId1", listOf("alice", "bob")))
    )

    describe("given error when fetching joined room members") {
        val networkError = Errors.network("test error")
        val mockPlatformClient by memoized {
            mockPlatformClient(
                    userSubscription(initialState),
                    justConnectingRoomSubscription(),
                    justConnectingCursorSubscription(),
                    usersFetchFailingWith(networkError)
            )
        }

        val subject by memoized {
            chatForFunctionalTest("alice", testPlatformClientFactory(mockPlatformClient))
        }

        describe("when room subscription is opened") {
            lateinit var errorOccured: FutureValue<RoomEvent.ErrorOccurred>
            beforeEachTest {
                errorOccured = subject.subscribeRoomFor("Room 1") { roomEvent ->
                    if (roomEvent is RoomEvent.ErrorOccurred) {
                        roomEvent
                    } else {
                        null
                    }
                }
            }

            it("then the error will be notified") {
                assertThat(errorOccured.get().error).isEqualTo(networkError)
            }
        }
    }

})