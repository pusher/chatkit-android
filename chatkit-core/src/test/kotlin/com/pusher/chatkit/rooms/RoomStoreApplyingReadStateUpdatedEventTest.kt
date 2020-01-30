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

    describe("given RoomStore with a Room with unreadCount of 0") {
        val initialRoom = simpleRoom("roomId1", "General")
        val subject by memoized {
            RoomStore().apply {
                initialiseContents(
                        listOf(initialRoom),
                        listOf(RoomMembershipApiType("roomId1", listOf("alice"))),
                        listOf(RoomReadStateApiType("roomId1", 0, null))
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

            it("then the result indicates that the room has been updated") {
                val roomUpdated: UserInternalEvent.RoomUpdated = result.assertTypedSingletonList()
                assertThat(roomUpdated.room.unreadCount).isEqualTo(1)
            }
            it("then the store contains the updated room") {
                assertThat(subject["roomId1"]!!.unreadCount).isEqualTo(1)
            }
        }
    }
})