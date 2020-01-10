package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.RoomMembershipApiType
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.users.UserSubscriptionEventParser
import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.Error
import elements.Subscription
import elements.SubscriptionEvent
import elements.emptyHeaders
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ReadStatusLimitFunctionalTest : Spek({

    describe("given two joined rooms with no read status for the second room " +
            "(simulating the top 1000 limit)") {

        // TODO: extract for reuse
        val dummySubscription = object : Subscription {
            override fun unsubscribe() {
                // nop
            }
        }
        val mockPlatformClient by memoized {
            mock<PlatformClient> {
                on { subscribeResuming(eq("users"), any(), any<UserSubscriptionEventParser>()) } doAnswer { invocation ->
                    Futures.schedule {
                        @Suppress("UNCHECKED_CAST")
                        val listener = invocation.arguments[1] as SubscriptionListeners<UserSubscriptionEvent>

                        listener.onEvent(SubscriptionEvent(
                                "initial_state",
                                emptyHeaders(),
                                UserSubscriptionEvent.InitialState(
                                        simpleUser("marek"),
                                        listOf(
                                                newEmptyJoinedRoom("roomId1", "1", "marek"),
                                                newEmptyJoinedRoomWithNoUnreadCount("roomId2", "2", "marek")
                                        ),
                                        listOf(
                                                ReadStateApiType("roomId1", 0, null)
                                        ),
                                        listOf(
                                                RoomMembershipApiType("roomId1", listOf("marek")),
                                                RoomMembershipApiType("roomId2", listOf("marek"))
                                        )
                                ))
                        )
                    }
                    dummySubscription
                }
            }
        }
        val testPlatformClientFactory = object : PlatformClientFactory {
            override fun createPlatformClient(instance: Instance, tokenProvider: TokenProvider): PlatformClient {
                return mockPlatformClient
            }
        }

        val subject by memoized { chatForFunctionalTest("marek", testPlatformClientFactory) }

        describe("when connect is called") {
            lateinit var connectResult: Result<SynchronousCurrentUser, Error>
            beforeEachTest {
                connectResult = subject.connect()
            }

            it("then the connect result is successful") {
                assertThat(connectResult).isInstanceOf(Result.Success::class.java)
            }
            it("then the first room has unread count") {
                assertThat(connectResult.successOrThrow().rooms[0].unreadCount).isEqualTo(0)
            }
            it("then the second room does not have unread count") {
                assertThat(connectResult.successOrThrow().rooms[1].unreadCount).isNull()
            }
        }
    }

})