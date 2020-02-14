package com.pusher.chatkit

import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.users.usersFetchFailingWith
import elements.Errors
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UserEnrichmentErrorFunctionalTest : Spek({

    val initialState = UserSubscriptionEvent.InitialState(
//            simpleUser("alice"),
            listOf(),
            listOf(),
            listOf()
//            listOf(simpleRoom("roomId1", "Room 1")),
//            listOf(RoomReadStateApiType("roomId1", 0)),
//            listOf(RoomMembershipApiType("roomId1", listOf("alice")))
    )
    val networkError = Errors.network("test error")

//    describe("given error when fetching user joining the room") {
//        val userJoinedEvent = UserSubscriptionEvent.UserJoinedRoomEvent("bob", "roomId1")
//
//        val mockPlatformClient = mockPlatformClient(
//                userSubscription(initialState, userJoinedEvent),
//                usersFetchFailingWith(networkError)
//        )
//
//        val subject = chatForFunctionalTest("alice", testPlatformClientFactory(mockPlatformClient))

//        describe("when connect is called") {
//            val notifiedEvents: BlockingQueue<ChatEvent> = LinkedBlockingQueue()
////            val connectResult = subject.connect { event -> notifiedEvents.add(event) }
//
//            it("then the connect result is successful\n" +
//                    "and the error is notified") {
//
////                assertThat(connectResult).isInstanceOf(Result.Success::class.java)
//
////                assertThat(notifiedEvents.take()).isInstanceOf(
////                        ChatEvent.CurrentUserReceived::class.java)
//
//                val errorOccurred = notifiedEvents.take() as ChatEvent.ErrorOccurred
//                assertThat(errorOccurred.error).isEqualTo(networkError)
//
//                assertWithMessage("Only 2 events expected")
//                        .that(notifiedEvents.poll(1, TimeUnit.MILLISECONDS)).isNull()
//            }
//        }
//    }
})
