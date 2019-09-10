package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomStore
import com.pusher.chatkit.users.User
import com.pusher.chatkit.users.UserSubscriptionEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class RoomStoreSpek : Spek({
    describe("RoomStore") {

        fun simpleRoom(id: String, name: String, isPrivate: Boolean, customData: CustomData?) =
                Room(id, "ham", name, null, isPrivate, customData, null, "2017-04-13T14:10:38Z",
                        "2017-04-13T14:10:38Z",
                        "2017-04-13T14:10:38Z",
                        "2017-04-13T14:10:38Z")

        describe("on receiving new InitialState User event") {
            val subject = RoomStore()

            val initialState = listOf(
                    simpleRoom("1", "one", false, null),
                    simpleRoom("2", "two", false, null),
                    simpleRoom("3", "three", false, null),
                    simpleRoom("4", "four", false, null),
                    simpleRoom("5", "five", false, null),
                    simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data")),
                    simpleRoom("8", "eight", false, mapOf("pre" to "set")),
                    simpleRoom("9", "nine", false, mapOf("pre" to "set"))
            )

            subject.initialiseContents(initialState)

            val replacementState = UserSubscriptionEvent.InitialState(
                    rooms = listOf(
                            simpleRoom("1", "one", false, null),
                            simpleRoom("3", "three", true, null),
                            simpleRoom("4", "four", false, mapOf("set" to "now")),
                            simpleRoom("5", "5ive", false, null),
                            simpleRoom("6", "size", false, null),
                            simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field")),
                            simpleRoom("8", "eight", false, null),
                            simpleRoom("9", "9ine", true, mapOf("pre" to "set", "and" to "updated"))
                    ),
                    cursors = listOf(),
                    currentUser = User("viv", "2017-04-13T14:10:04Z", "2017-04-13T14:10:04Z", "Vivan", null, mapOf("email" to "vivan@pusher.com"))
            )

            val replacementEvents = subject.applyUserSubscriptionEvent(replacementState)

            it("should emit expected hooks") {
                assertThat(replacementEvents).containsExactly(
                        UserSubscriptionEvent.RemovedFromRoomEvent("2"),
                        UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("3", "three", true, null)),
                        UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("4", "four", false, mapOf("set" to "now"))),
                        UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("5", "5ive", false, null)),
                        UserSubscriptionEvent.AddedToRoomEvent(simpleRoom("6", "size", false, null)),
                        UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field"))),
                        UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("8", "eight", false, null)),
                        UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("9", "9ine", true, mapOf("pre" to "set", "and" to "updated"))),
                        replacementState // this event is not removed during the expansion because the roomStore has not dealt with the currentUser field
                )
            }

            it("should update the room store") {
                val roomStoreContents = subject.toList().sortedBy { it.id }

                assertThat(roomStoreContents).hasSize(8)

                val differences = roomStoreContents.zip(listOf(
                        simpleRoom("1", "one", false, null),
                        simpleRoom("3", "three", true, null),
                        simpleRoom("4", "four", false, mapOf("set" to "now")),
                        simpleRoom("5", "5ive", false, null),
                        simpleRoom("6", "size", false, null),
                        simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field")),
                        simpleRoom("8", "eight", false, null),
                        simpleRoom("9", "9ine", true, mapOf("pre" to "set", "and" to "updated"))
                )).filterNot { (l, r) -> l.deepEquals(r) }

                assertThat(differences).isEmpty()
            }
        }

        describe("Member user ids are consistent") {

            val subject = RoomStore()

            val roomOne = simpleRoom("1", "one", true, null)

            val initialState = listOf(roomOne)
            subject.initialiseContents(initialState)

            subject.applyMembershipEvent(
                    "1",
                    MembershipSubscriptionEvent.InitialState(
                            listOf("viv", "mike")
                    )
            )

            val roomOneUpdated = simpleRoom("1", "new", true, null)
            val roomTwoNew = simpleRoom("2", "wasn't there before", false, null)

            val replacementState = UserSubscriptionEvent.InitialState(
                    rooms = listOf(roomOneUpdated, roomTwoNew),
                    cursors = listOf(),
                    currentUser = User("viv", "2017-04-13T14:10:04Z",
                            "2017-04-13T14:10:04Z", "Vivan", null, mapOf("email" to "vivan@pusher.com"))
            )

            val replacementEvents = subject.applyUserSubscriptionEvent(replacementState)

            assertThat(replacementEvents).hasSize(3)

            for (event in replacementEvents) {
                when (event) {
                    is UserSubscriptionEvent.RoomUpdatedEvent -> {
                        assertThat(event.room.name).isEqualTo("new")
                        assertThat(event.room.memberUserIds).containsExactly("viv", "mike")
                    }
                    is UserSubscriptionEvent.AddedToRoomEvent -> {
                        assertThat(event.room.name).isEqualTo("wasn't there before")
                        assertThat(event.room.memberUserIds.size).isEqualTo(0)
                    }
                    is UserSubscriptionEvent.InitialState -> {}
                    else -> { // todo fail!
                    }
                }
            }
        }

        describe("On receiving new InitialState Membership event") {
            val subject = RoomStore()

            val room = simpleRoom("1", "one", false, null)

            subject += room

            val events = subject.applyMembershipEvent(
                    room.id,
                    MembershipSubscriptionEvent.InitialState(listOf("mike", "callum", "alice"))
            )

            it ("should not emit any joined events in the initial state") {
                assertThat(events).isEmpty()
            }

            val eventJoined = subject.applyMembershipEvent(room.id,
                    MembershipSubscriptionEvent.UserJoined("bob"))
            it ("should emit new joiner event") {
                assertThat(eventJoined).containsExactly(MembershipSubscriptionEvent.UserJoined("bob"))
            }

            val eventLeft = subject.applyMembershipEvent(room.id,
                    MembershipSubscriptionEvent.UserLeft("alice"))
            it ("should emit person left event") {
                assertThat(eventLeft).containsExactly(MembershipSubscriptionEvent.UserLeft("alice"))
            }

            it("should update the room membership") {
                assertThat(room.memberUserIds).containsExactly("mike", "callum", "bob")
            }
        }
    }
})
