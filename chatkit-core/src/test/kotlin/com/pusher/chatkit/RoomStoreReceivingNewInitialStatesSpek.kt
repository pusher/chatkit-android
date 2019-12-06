package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomStore
import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.RoomMembershipApiType
import com.pusher.chatkit.users.User
import com.pusher.chatkit.users.UserSubscriptionEvent
import org.junit.Assert.fail
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomStoreReceivingNewInitialStatesSpek : Spek({
    val subject by memoized { RoomStore() }

    describe("on receiving new InitialState from user's subscription") {
        lateinit var addedRoom6: Room
        lateinit var readStateForAddedRoom6: ReadStateApiType

        lateinit var unreadCountChangedForRoom10: ReadStateApiType

        lateinit var replacementState: UserSubscriptionEvent.InitialState
        lateinit var replacementEvents: List<UserSubscriptionEvent>

        @Suppress("MapGetWithNotNullAssertionOperator")
        beforeEachTest {
            val readStates = mapOf(
                    "1" to ReadStateApiType("1", 11, null),
                    "2" to ReadStateApiType("2", 22, null),
                    "3" to ReadStateApiType("3", 33, null),
                    "4" to ReadStateApiType("4", 44, null),
                    "5" to ReadStateApiType("5", 55, null),
                    "7" to ReadStateApiType("7", 77, null),
                    "8" to ReadStateApiType("8", 88, null),
                    "9" to ReadStateApiType("9", 99, null)
            )
            val roomOneUnchanged = simpleRoom("1", "one", false, null, readStates["1"]!!.unreadCount)
            val initialState = listOf(
                    roomOneUnchanged,
                    simpleRoom("2", "two", false, null, readStates["2"]!!.unreadCount),
                    simpleRoom("3", "three", false, null, readStates["3"]!!.unreadCount),
                    simpleRoom("4", "four", false, null, readStates["4"]!!.unreadCount),
                    simpleRoom("5", "five", false, null, readStates["5"]!!.unreadCount),
                    simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data"), readStates["7"]!!.unreadCount),
                    simpleRoom("8", "eight", false, mapOf("pre" to "set"), readStates["8"]!!.unreadCount),
                    simpleRoom("9", "nine", false, mapOf("pre" to "set"), readStates["9"]!!.unreadCount),
                    simpleRoom("10", "ten", false, null, 100) // unread count will change here
            )
            subject.initialiseContents(initialState)

            addedRoom6 = simpleRoom("6", "size", false, null)
            readStateForAddedRoom6 = ReadStateApiType(addedRoom6.id, 66, null)

            unreadCountChangedForRoom10 = ReadStateApiType("10", 101, null)

            // TODO: add membership change related test (best breaking those tests so that
            //  each test actually tests only one thing)

            replacementState = UserSubscriptionEvent.InitialState(
                    currentUser = User("viv", "2017-04-13T14:10:04Z", "2017-04-13T14:10:04Z",
                            "Vivan", null, mapOf("email" to "vivan@pusher.com")),
                    _rooms = listOf(
                            roomOneUnchanged,
                            simpleRoom("3", "three", true, null),
                            simpleRoom("4", "four", false, mapOf("set" to "now")),
                            simpleRoom("5", "5ive", false, null),
                            addedRoom6,
                            simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field")),
                            simpleRoom("8", "eight", false, null),
                            simpleRoom("9", "9ine", true, mapOf("pre" to "set", "and" to "updated")),
                            simpleRoom("10", "ten", false, null)
                    ),
                    readStates = readStates.values + readStateForAddedRoom6 + unreadCountChangedForRoom10,
                    memberships = listOf(
                        RoomMembershipApiType("1", listOf("viv")),
                        RoomMembershipApiType("3", listOf("viv")),
                        RoomMembershipApiType("4", listOf("viv")),
                        RoomMembershipApiType("5", listOf("viv")),
                        RoomMembershipApiType("6", listOf("viv")),
                        RoomMembershipApiType("7", listOf("viv")),
                        RoomMembershipApiType("8", listOf("viv")),
                        RoomMembershipApiType("9", listOf("viv")),
                        RoomMembershipApiType("10", listOf("viv"))
                    )
            )
            replacementEvents = subject.applyUserSubscriptionEvent(replacementState)
        }

        it("will emit expected events") {
            assertThat(replacementEvents).containsExactly(
                    UserSubscriptionEvent.RemovedFromRoomEvent("2"),
                    UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("3", "three", true, null, 0)),
                    UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("4", "four", false, mapOf("set" to "now"), 0)),
                    UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("5", "5ive", false, null, 0)),
                    UserSubscriptionEvent.AddedToRoomEvent(addedRoom6, readStateForAddedRoom6, RoomMembershipApiType("6", listOf("viv"))),
                    UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field"), 0)),
                    UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("8", "eight", false, null, 0)),
                    UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("9", "9ine", true, mapOf("pre" to "set", "and" to "updated"), 0)),
                    UserSubscriptionEvent.RoomUpdatedEvent(simpleRoom("10", "ten", false, null, 101)),
                    replacementState // this event is not removed during the expansion because the roomStore has not dealt with the currentUser field
            )
        }

        it("will update the room store") {
            val actualRooms = subject.toList().sortedBy { it.id.toInt() }

            assertThat(actualRooms).hasSize(9)

            val expectedRooms = listOf(
                    simpleRoom("1", "one", false, null, 11),
                    simpleRoom("3", "three", true, null, 33),
                    simpleRoom("4", "four", false, mapOf("set" to "now"), 44),
                    simpleRoom("5", "5ive", false, null, 55),
                    addedRoom6.copy(unreadCount = 66),
                    simpleRoom("7", "seven", false, mapOf("pre" to "set", "custom" to "data", "third" to "field"), 77),
                    simpleRoom("8", "eight", false, null, 88),
                    simpleRoom("9", "9ine", true, mapOf("pre" to "set", "and" to "updated"), 99),
                    simpleRoom("10", "ten", false, null, 101)
            )

            val differences = actualRooms.zip(expectedRooms)
                    .filterNot { (a, e) -> a.deepEquals(e) }
                    .onEach { (a, e) ->
                        System.err.println("Not matching room, actual:\n$a\nexpected:\n$e") }

            assertThat(differences).isEmpty()
        }
    }

    describe("on receiving new InitialState from user's subscription with updated and added room") {
        lateinit var replacementEvents: List<UserSubscriptionEvent>

        beforeEachTest {
            val roomOneReadState = ReadStateApiType("1", 0, null)
            val roomOne = simpleRoom("1", "one", true, null, roomOneReadState.unreadCount)
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
                    currentUser = User("viv", "2017-04-13T14:10:04Z",
                            "2017-04-13T14:10:04Z", "Vivan", null, mapOf("email" to "vivan@pusher.com")),
                    _rooms = listOf(roomOneUpdated, roomTwoNew),
                    readStates = listOf(roomOneReadState, ReadStateApiType("2", 0, null)),
                    memberships = listOf(
                            RoomMembershipApiType("1", listOf("viv", "mike")),
                            RoomMembershipApiType("2", listOf("viv"))
                    )
            )
            replacementEvents = subject.applyUserSubscriptionEvent(replacementState)
        }

        it ("will emit only relevant events") {
            assertThat(replacementEvents).hasSize(3)

            for (event in replacementEvents) {
                when (event) {
                    is UserSubscriptionEvent.RoomUpdatedEvent -> {
                        assertThat(event.room.name).isEqualTo("new")
                        assertThat(event.room.memberUserIds).containsExactly("viv", "mike")
                    }
                    is UserSubscriptionEvent.AddedToRoomEvent -> {
                        assertThat(event.room.name).isEqualTo("wasn't there before")
                        assertThat(event.room.memberUserIds).containsExactly("viv")
                    }
                    is UserSubscriptionEvent.InitialState -> {}
                    else -> fail()
                }
            }
        }
    }

    // TODO: adjust as in v7 it's part of user sub
    // TODO: make the tests here have common reused and meaningful given part and clear separation
    //  between given and when parts
    describe("on receiving new InitialState from room membership subscription") {
        lateinit var events: List<MembershipSubscriptionEvent>

        beforeEachTest {
            val room = simpleRoom("1", "one", false, null)
                    .copy(memberUserIds = setOf("callum", "mike", "alice"))

            subject += room

            events = subject.applyMembershipEvent(
                    room.id,
                    MembershipSubscriptionEvent.InitialState(listOf("mike", "callum", "bob"))
            )
        }

        it("will emit the correct events") {
            assertThat(events).containsExactly(
                    MembershipSubscriptionEvent.UserJoined("bob"),
                    MembershipSubscriptionEvent.UserLeft("alice")
            )
        }

        it("will update the room membership") {
            assertThat(subject["1"]!!.memberUserIds).containsExactly("mike", "callum", "bob")
        }
    }
})

