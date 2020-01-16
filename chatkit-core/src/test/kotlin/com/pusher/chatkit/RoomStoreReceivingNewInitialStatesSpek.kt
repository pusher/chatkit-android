package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.rooms.RoomStore
import com.pusher.chatkit.rooms.api.RoomApiType
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.users.User
import com.pusher.chatkit.users.UserInternalEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomStoreReceivingNewInitialStatesSpek : Spek({
    val subject by memoized { RoomStore() }

    val initialReadStates = listOf(
            RoomReadStateApiType("1", 11, null),
            RoomReadStateApiType("2", 22, null),
            RoomReadStateApiType("3", 33, null)
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
            readStates: List<RoomReadStateApiType>? = null,
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
                val newReadState = RoomReadStateApiType("new", 5, null)
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
            describe("replacement state with a difference in unread count") {
                val newReadState = RoomReadStateApiType(initialRooms[1].id, 23, null)
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

            describe("replacement state with a read state removed") {
                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            readStates = listOf(initialReadStates[0], initialReadStates[2])
                    )
                }

                it("reports nothing") {
                    assertThat(events).hasSize(0)
                }
            }

            describe("replacement state with a read state not matching a room (technically a backend error)") {
                lateinit var events: List<UserInternalEvent>
                val newReadState = RoomReadStateApiType("unknown", 1, null)

                beforeEachTest {
                    events = applyInitialState(
                            readStates = initialReadStates + newReadState
                    )
                }

                it("reports nothing") {
                    assertThat(events).hasSize(0)
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

        describe("multiple differences") {
            describe("replacement state with difference in read state and room last message timestamp") {
                val newReadState = RoomReadStateApiType(initialRooms[1].id, 23, null)
                val newRoom = with (initialRooms[1]) {
                            simpleRoom(
                                    id = id,
                                    name = name,
                                    customData = customData,
                                    isPrivate = private,
                                    lastMessageAt = "2020-01-07T14:10:38Z"
                            )
                        }

                lateinit var events: List<UserInternalEvent>

                beforeEachTest {
                    events = applyInitialState(
                            rooms = listOf(initialRooms[0], newRoom, initialRooms[2]),
                            readStates = listOf(initialReadStates[0], newReadState, initialReadStates[2])
                    )
                }

                it("reports a single RoomUpdated, with the new unreadCount and lastMessageAt visible") {
                    val event = events.assertSingletonListWithElementType(UserInternalEvent.RoomUpdated::class.java)
                    assertThat(event.room.id).isEqualTo(newRoom.id)
                    assertThat(event.room.lastMessageAt).isEqualTo(newRoom.lastMessageAt)
                    assertThat(event.room.unreadCount).isEqualTo(newReadState.unreadCount)
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
