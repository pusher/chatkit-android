package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.util.FutureValue
import com.pusher.util.Result
import elements.Error
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ReadStatusLimitFunctionalTest : Spek({

    val initialState = UserSubscriptionEvent.InitialState(
            simpleUser("marek"),
            listOf(
                    simpleRoom("roomId1", "1", lastMessageAt = "2017-05-14T14:10:38Z"),
                    simpleRoom("roomId2", "2", lastMessageAt = null)
            ),
            listOf(
                    RoomReadStateApiType("roomId1", 0)
            ),
            listOf(
                    RoomMembershipApiType("roomId1", listOf("marek")),
                    RoomMembershipApiType("roomId2", listOf("marek"))
            )
    )

    describe("given two joined rooms with no read status for the second room " +
            "(simulating the top 1000 limit)") {

        val mockPlatformClient by memoized {
            mockPlatformClientForUserSubscription(initialState)
        }

        val subject by memoized {
            chatForFunctionalTest("marek", testPlatformClientFactory(mockPlatformClient))
        }

//        describe("when connect is called") {
//            lateinit var connectResult: Result<SynchronousCurrentUser, Error>
//            beforeEachTest {
//                connectResult = subject.connect()
//            }
//
//            it("then the connect result is successful") {
//                assertThat(connectResult).isInstanceOf(Result.Success::class.java)
//            }
//            it("then the first room has unread count") {
//                assertThat(connectResult.successOrThrow().rooms[0].unreadCount).isEqualTo(0)
//            }
//            it("then the second room does not have unread count") {
//                assertThat(connectResult.successOrThrow().rooms[1].unreadCount).isNull()
//            }
//        }
    }

    describe("given read_state_updated event for the second room " +
            "(the one with no unread count)") {

        val readStateUpdatedEvent = UserSubscriptionEvent.ReadStateUpdatedEvent(
                RoomReadStateApiType("roomId2", 1)
        )
        val mockPlatformClient by memoized {
            mockPlatformClientForUserSubscription(initialState, readStateUpdatedEvent)
        }

        val subject by memoized {
            chatForFunctionalTest("marek", testPlatformClientFactory(mockPlatformClient))
        }

//        describe("when connect is called") {
//            lateinit var connectResult: Result<SynchronousCurrentUser, Error>
//            lateinit var roomUpdatedEvent: FutureValue<ChatEvent.RoomUpdated>
//            beforeEachTest {
//                roomUpdatedEvent = FutureValue()
//
//                connectResult = subject.connect { chatEvent ->
//                    if (chatEvent is ChatEvent.RoomUpdated) roomUpdatedEvent.set(chatEvent)
//                }
//            }
//
//            it("then the expected room updated event is notified") {
//                assertThat(roomUpdatedEvent.get().room.id).isEqualTo("roomId2")
//                assertThat(roomUpdatedEvent.get().room.unreadCount).isEqualTo(1)
//            }
//            it("then the second room has unread count") {
//                roomUpdatedEvent.get() // wait for the event first
//                assertThat(connectResult.successOrThrow().rooms[1].unreadCount).isEqualTo(1)
//            }
//            it("then the first room has its unread count unchanged") {
//                roomUpdatedEvent.get() // wait for the event first
//                assertThat(connectResult.successOrThrow().rooms[0].unreadCount).isEqualTo(0)
//            }
//        }
    }
})
