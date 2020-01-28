package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.users.usersFetchFailingWith
import com.pusher.util.Result
import elements.Error
import elements.Errors
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object UserEnrichmentErrorFunctionalTest : Spek({

    val initialState = UserSubscriptionEvent.InitialState(
            simpleUser("alice"),
            listOf(simpleRoom("roomId1", "Room 1")),
            listOf(RoomReadStateApiType("roomId1", 0, null)),
            listOf(RoomMembershipApiType("roomId1", listOf("alice")))
    )
    val networkError = Errors.network("test error")

    describe("given error when fetching user joining the room") {
        val userJoinedEvent = UserSubscriptionEvent.UserJoinedRoomEvent("bob", "roomId1")

        val mockPlatformClient by memoized {
            mockPlatformClient(
                    userSubscription(initialState, userJoinedEvent),
                    usersFetchFailingWith(networkError)
            )
        }

        val subject by memoized {
            chatForFunctionalTest("alice", testPlatformClientFactory(mockPlatformClient))
        }

        describe("when connect is called") {
            lateinit var connectResult: Result<SynchronousCurrentUser, Error>

            val notifiedEvents: BlockingQueue<ChatEvent> = LinkedBlockingQueue()
            afterEachTest { notifiedEvents.clear() }

            beforeEachTest {
                connectResult = subject.connect { event -> notifiedEvents.add(event) }
            }

            it("then the connect result is successful") {
                assertThat(connectResult).isInstanceOf(Result.Success::class.java)
            }
            it("then the error is notified") {
                assertThat(notifiedEvents.take()).isInstanceOf(
                        ChatEvent.CurrentUserReceived::class.java)

                val errorOccurred = notifiedEvents.take() as ChatEvent.ErrorOccurred
                assertThat(errorOccurred.error).isEqualTo(networkError)

                assertWithMessage("Only 2 events expected")
                        .that(notifiedEvents.poll(1, TimeUnit.MILLISECONDS)).isNull()
            }
        }
    }

})