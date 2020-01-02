package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.rooms.RoomStore
import com.pusher.chatkit.users.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomStoreReceivingNewInitialStatesSpek : Spek({
    val subject by memoized { RoomStore() }

    val initialReadStates = listOf(
            ReadStateApiType("1", 11, null),
            ReadStateApiType("2", 22, null),
            ReadStateApiType("3", 33, null)
    )
    val initialMemberships = listOf(
            RoomMembershipApiType("1", listOf("viv")),
            RoomMembershipApiType("2", listOf("viv", "ham")),
            RoomMembershipApiType("3", listOf("viv"))
    )
    val initialRooms = listOf(
            simpleRoom("1", "one", false, null),
            simpleRoom("2", "two", true, null),
            simpleRoom("3", "three", false, mapOf("custom" to "data"))
    )

    val currentUser = User(
            id = "testUser",
            name = "Test User",
            createdAt = "blah",
            updatedAt = "blah",
            customData = null,
            avatarURL = null
    )

    fun applyInitialState(
            rooms: List<RoomApiType>? = null,
            readStates: List<ReadStateApiType>? = null,
            memberships: List<RoomMembershipApiType>? = null
    ) =
            subject.applyUserSubscriptionEvent(
                    UserSubscriptionEvent.InitialState(
                            currentUser = currentUser,
                            rooms = rooms ?: initialRooms,
                            readStates = readStates ?: initialReadStates,
                            memberships = memberships ?: initialMemberships
                    )
            )

    describe("A RoomStore") {
        beforeEachTest {
            subject.initialiseContents(
                    initialRooms,
                    memberships = initialMemberships,
                    readStates = initialReadStates
            )
        }

        describe("replacement state with no changes") {
            lateinit var events: List<UserInternalEvent>
            beforeEachTest {
                events = subject.applyUserSubscriptionEvent(
                        UserSubscriptionEvent.InitialState(
                                currentUser = currentUser,
                                rooms = initialRooms,
                                memberships = initialMemberships,
                                readStates = initialReadStates
                        )
                )
            }

            it("reports no changes") {
                assertThat(events).isEmpty()
            }
        }

        describe("differences in room entities") {
            describe("replacement state with a difference in a room") {
                val newRoom = simpleRoom(initialRooms[1].id, "number 2", true, null)
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            rooms = listOf(initialRooms[0], newRoom, initialRooms[2])
                    )
                }

                it("reports a RoomUpdated, with the new field visible") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.RoomUpdated::class.java)
                    assertThat(event.room.id).isEqualTo(newRoom.id)
                    assertThat(event.room.name).isEqualTo(newRoom.name)
                }
            }

            describe("replacement state with a new room, including memberships and read state") {
                val newRoom = simpleRoom("new", "New Room", false, null)
                val newMembers = RoomMembershipApiType("new", listOf("member_a", "member_b"))
                val newReadState = ReadStateApiType("new", 5, null)
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            rooms = initialRooms + newRoom,
                            memberships = initialMemberships + newMembers,
                            readStates = initialReadStates + newReadState
                    )
                }

                it("reports an AddedToRoom event with the room attached") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.AddedToRoom::class.java)
                    assertThat(event.room.id).isEqualTo(newRoom.id)
                    assertThat(event.room.name).isEqualTo(newRoom.name)
                }

                it("sets the members on the Room in the event") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.AddedToRoom::class.java)
                    assertThat(event.room.memberUserIds).containsExactlyElementsIn(newMembers.userIds)
                }

                it("sets the unread count on the Room in the event") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.AddedToRoom::class.java)
                    assertThat(event.room.unreadCount).isEqualTo(newReadState.unreadCount)
                }
            }

            describe("replacement state with a new room, including memberships only") {
                val newRoom = simpleRoom("new", "New Room", false, null)
                val newMembers = RoomMembershipApiType("new", listOf("member_a", "member_b"))
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            rooms = initialRooms + newRoom,
                            memberships = initialMemberships + newMembers
                    )
                }

                it("reports an AddedToRoom event with the room attached") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.AddedToRoom::class.java)
                    assertThat(event.room.id).isEqualTo(newRoom.id)
                    assertThat(event.room.name).isEqualTo(newRoom.name)
                }

                it("sets the members on the Room in the event") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.AddedToRoom::class.java)
                    assertThat(event.room.memberUserIds).containsExactlyElementsIn(newMembers.userIds)
                }

                it("nulls the unread count on the Room in the event") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.AddedToRoom::class.java)
                    assertThat(event.room.unreadCount).isNull()
                }
            }

            describe("replacement state with a room removed") {
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            rooms = listOf(initialRooms[0], initialRooms[2])
                    )
                }

                it("reports a RoomUpdated, with the new field visible") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.RemovedFromRoom::class.java)
                    assertThat(event.roomId).isEqualTo(initialRooms[1].id)
                }
            }
        }

        describe("differences in read states") {
            describe("replacement state with a difference in read state") {
                val newReadState = ReadStateApiType(initialRooms[1].id, 23, null)
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            readStates = listOf(initialReadStates[0], newReadState, initialReadStates[2])
                    )
                }

                it("reports a RoomUpdated, with the new unreadCount visible") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.RoomUpdated::class.java)
                    assertThat(event.room.id).isEqualTo(newReadState.roomId)
                    assertThat(event.room.unreadCount).isEqualTo(newReadState.unreadCount)
                }
            }
        }

        describe("differences in memberships") {
            describe("replacement state with a new member added") {
                val newMemberships = RoomMembershipApiType(initialRooms[0].id, listOf("mike", "viv"))
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            memberships = listOf(newMemberships, initialMemberships[1], initialMemberships[2])
                    )
                }

                it("reports a UserJoinedRoom, with the new member") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.UserJoinedRoom::class.java)
                    assertThat(event.roomId).isEqualTo(newMemberships.roomId)
                    assertThat(event.userId).isEqualTo(newMemberships.userIds[0])
                }
            }

            describe("replacement state with a member removed") {
                val newMemberships = RoomMembershipApiType(initialRooms[1].id, listOf("viv"))
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            memberships = listOf(initialMemberships[0], newMemberships, initialMemberships[2])
                    )
                }

                it("reports a UserLeftRoom, with the old member") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.UserLeftRoom::class.java)
                    assertThat(event.roomId).isEqualTo(newMemberships.roomId)
                    assertThat(event.userId).isEqualTo("ham")
                }
            }
        }
    }
})

private fun <V, T: V> List<V>.assertSingletonListWithElementType(clazz: Class<T>): T {
    assertThat(this).hasSize(1)
    assertThat(this.first()).isInstanceOf(clazz)
    return this.first() as T
}
