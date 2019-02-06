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
        describe("on receiving new InitialState User event") {
            val subject = RoomStore()

            val initialState = UserSubscriptionEvent.InitialState(
                    rooms = listOf(
                            Room("1", "ham", "one", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("2", "ham", "two", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("3", "ham", "three", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("4", "ham", "four", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("5", "ham", "five", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("7", "ham", "seven", false, mapOf("pre" to "set", "custom" to "data"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("8", "ham", "eight", false, mapOf("pre" to "set"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("9", "ham", "nine", false, mapOf("pre" to "set"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")
                    ),
                    currentUser = User("viv", "2017-04-13T14:10:04Z", "2017-04-13T14:10:04Z", "Vivan", null, mapOf("email" to "vivan@pusher.com"))
            )

            subject.applyUserSubscriptionEvent(initialState)

            val replacementState = UserSubscriptionEvent.InitialState(
                    rooms = listOf(
                            Room("1", "ham", "one", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("3", "ham", "three", true, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("4", "ham", "four", false, mapOf("set" to "now"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("5", "ham", "5ive", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("6", "ham", "size", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("7", "ham", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("8", "ham", "eight", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                            Room("9", "ham", "9ine", true, mapOf("pre" to "set", "and" to "updated"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")
                    ),
                    currentUser = User("viv", "2017-04-13T14:10:04Z", "2017-04-13T14:10:04Z", "Vivan", null, mapOf("email" to "vivan@pusher.com"))
            )

            val replacementEvents = subject.applyUserSubscriptionEvent(replacementState)

            it("should emit expected hooks") {
                assertThat(replacementEvents).containsExactly(
                        UserSubscriptionEvent.RemovedFromRoomEvent("2"),
                        UserSubscriptionEvent.RoomUpdatedEvent(Room("3", "ham", "three", true, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")),
                        UserSubscriptionEvent.RoomUpdatedEvent(Room("4", "ham", "four", false, mapOf("set" to "now"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")),
                        UserSubscriptionEvent.RoomUpdatedEvent(Room("5", "ham", "5ive", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")),
                        UserSubscriptionEvent.AddedToRoomEvent(Room("6", "ham", "size", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")),
                        UserSubscriptionEvent.RoomUpdatedEvent(Room("7", "ham", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")),
                        UserSubscriptionEvent.RoomUpdatedEvent(Room("8", "ham", "eight", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")),
                        UserSubscriptionEvent.RoomUpdatedEvent(Room("9", "ham", "9ine", true, mapOf("pre" to "set", "and" to "updated"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")),
                        replacementState // this event is not removed during the expansion because the roomStore has not dealt with the currentUser field
                )
            }

            it("should update the room store") {
                val roomStoreContents = subject.toList().sortedBy { it.id }

                assertThat(roomStoreContents).hasSize(8)

                val differences = roomStoreContents.zip(listOf(
                        Room("1", "ham", "one", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                        Room("3", "ham", "three", true, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                        Room("4", "ham", "four", false, mapOf("set" to "now"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                        Room("5", "ham", "5ive", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                        Room("6", "ham", "size", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                        Room("7", "ham", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                        Room("8", "ham", "eight", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", ""),
                        Room("9", "ham", "9ine", true, mapOf("pre" to "set", "and" to "updated"), "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")
                )).filterNot { (l, r) -> l.deepEquals(r) }

                assertThat(differences).isEmpty()
            }
        }

        describe("On receiving new InitialState Membership event") {
            val subject = RoomStore()

            val room = Room("1", "ham", "one", false, null, "2017-04-13T14:10:38Z", "2017-04-13T14:10:38Z", "")
            setOf("callum", "mike", "alice").forEach { room.addUser(it) }

            subject += room

            val events = subject.applyMembershipEvent(
                    room.id,
                    MembershipSubscriptionEvent.InitialState(listOf("mike", "callum", "bob"))
            )

            it("should emit the correct events") {
                assertThat(events).containsExactly(
                        MembershipSubscriptionEvent.UserJoined("bob"),
                        MembershipSubscriptionEvent.UserLeft("alice")
                )
            }

            it("should update the room membership") {
                assertThat(room.memberUserIds).containsExactly("mike", "callum", "bob")
            }
        }
    }
})