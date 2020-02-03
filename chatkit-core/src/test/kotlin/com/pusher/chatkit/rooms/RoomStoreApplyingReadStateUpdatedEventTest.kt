package com.pusher.chatkit.rooms

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.assertTypedSingletonList
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.simpleRoom
import com.pusher.chatkit.users.UserInternalEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object RoomStoreApplyingReadStateUpdatedEventTest : Spek({

    describe("given RoomStore with 3 Rooms with varied unreadCounts") {
        val initialRoom1 = simpleRoom("roomId1", "General")
        val initialRoom2 = simpleRoom("roomId2", "Kotlin")
        val initialRoom3 = simpleRoom("roomId1001", "Old General")
        val subject by memoized {
            RoomStore().apply {
                initialiseContents(
                        listOf(initialRoom1, initialRoom2, initialRoom3),
                        listOf(
                                RoomMembershipApiType("roomId1", listOf("alice")),
                                RoomMembershipApiType("roomId2", listOf("alice")),
                                RoomMembershipApiType("roomId1001", listOf("alice"))
                        ),
                        listOf(
                                RoomReadStateApiType("roomId1", unreadCount = 0, cursor = null),
                                RoomReadStateApiType("roomId2", unreadCount = 2, cursor = null)
                                // the 3rd room missing unreadCount (simulating the top 1000 limit)
                        )
                )
            }
        }

        describe("when applying ReadStateUpdatedEvent with new unreadCount") {
            lateinit var result: List<UserInternalEvent>
            beforeEachTest {
                result = subject.applyUserSubscriptionEvent(
                        UserSubscriptionEvent.ReadStateUpdatedEvent(
                            RoomReadStateApiType(
                                    roomId = "roomId1",
                                    unreadCount = 1,
                                    cursor = null
                            )
                        )
                )
            }

            it("then the result indicates that the expected room has been updated") {
                val roomUpdated: UserInternalEvent.RoomUpdated = result.assertTypedSingletonList()
                assertThat(roomUpdated.room.id).isEqualTo("roomId1")
                assertThat(roomUpdated.room.unreadCount).isEqualTo(1)
            }
            it("then the store contains the updated room") {
                assertThat(subject["roomId1"]!!.unreadCount).isEqualTo(1)
            }
            it("then the store contains the non-updated rooms") {
                assertThat(subject["roomId2"]!!.unreadCount).isEqualTo(2)
                assertThat(subject["roomId1001"]!!.unreadCount).isNull()
            }
        }
    }
})