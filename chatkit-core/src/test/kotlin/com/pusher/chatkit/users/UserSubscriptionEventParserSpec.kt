package com.pusher.chatkit.users

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.TestFileReader
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.users.UserSubscriptionEvent.AddedToRoomEvent
import com.pusher.chatkit.users.UserSubscriptionEvent.InitialState
import com.pusher.chatkit.users.UserSubscriptionEvent.UserJoinedRoomEvent
import com.pusher.chatkit.users.UserSubscriptionEvent.UserLeftRoomEvent
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UserSubscriptionEventParserSpec : Spek({
    val testFileReader = TestFileReader("/json/subscription/user")

    describe("when parsing initial state example event from the docs") {
        val initialStateEvent = UserSubscriptionEventParser(
                testFileReader.readTestFile("initial_state-docs.json")
        ).successOrThrow() as InitialState

        it("then result's memberships contain valid information") {
            val memberships = initialStateEvent.memberships

            assertThat(memberships.size).isEqualTo(1)

            assertThat(memberships[0].roomId).isEqualTo("cool-room-1")
            assertThat(memberships[0].userIds.size).isEqualTo(2)
            assertThat(memberships[0].userIds[0]).isEqualTo("jean")
            assertThat(memberships[0].userIds[1]).isEqualTo("ham")
        }

        it("then result's read states contains valid information") {
            val expectedReadState = RoomReadStateApiType(
                    roomId = "cool-room-1",
                    unreadCount = 7,
                    cursor = Cursor(
                            userId = "viv",
                            roomId = "cool-room-1",
                            position = 123654,
                            updatedAt = "2017-04-13T14:10:04Z",
                            type = 0
                    )
            )

            assertThat(initialStateEvent.readStates).containsExactly(expectedReadState)
        }
    }

    describe("when parsing added to room example event from the docs") {
        val addedToRoomEvent = UserSubscriptionEventParser(
                testFileReader.readTestFile("added_to_room-docs.json")
        ).successOrThrow() as AddedToRoomEvent

        it("then result's memberships will contain valid information") {
            val membership = addedToRoomEvent.membership
            assertThat(membership.roomId).isEqualTo("cool-room-2")
            assertThat(membership.userIds.size).isEqualTo(1)
            assertThat(membership.userIds[0]).isEqualTo("ham")
        }

        it("then result's read states contains valid information") {
            val expectedReadState = RoomReadStateApiType(
                    roomId = "cool-room-2",
                    unreadCount = 15,
                    cursor = Cursor(
                            userId = "viv",
                            roomId = "cool-room-2",
                            position = 123654,
                            updatedAt = "2017-04-13T14:10:04Z",
                            type = 0
                    )
            )

            assertThat(addedToRoomEvent.readState).isEqualTo(expectedReadState)
        }
    }

    describe("when parsing user joined room example event from the docs") {
        val userJoinedRoomEvent = UserSubscriptionEventParser(
                testFileReader.readTestFile("user_joined_room-docs.json")
        ).successOrThrow() as UserJoinedRoomEvent

        it("then result has valid user/room IDs") {
            assertThat(userJoinedRoomEvent.userId).isEqualTo("xavier")
            assertThat(userJoinedRoomEvent.roomId).isEqualTo("cool-room-1")
        }
    }

    describe("when parsing user left room example event from the docs") {
        val userJoinedRoomEvent = UserSubscriptionEventParser(
                testFileReader.readTestFile("user_left_room-docs.json")
        ).successOrThrow() as UserLeftRoomEvent

        it("then result has valid user/room IDs") {
            assertThat(userJoinedRoomEvent.userId).isEqualTo("xavier")
            assertThat(userJoinedRoomEvent.roomId).isEqualTo("cool-room-1")
        }
    }

    describe("when parsing initial state with no cursor") {
        val initialStateEvent = UserSubscriptionEventParser(
                testFileReader.readTestFile("initial_state-withNoCursor.json")
        ).successOrThrow() as InitialState

        it("then result's read states contains valid information") {
            val expectedReadState = RoomReadStateApiType(
                    roomId = "cool-room-1",
                    unreadCount = 7,
                    cursor = null
            )

            assertThat(initialStateEvent.readStates).containsExactly(expectedReadState)
        }
    }

    describe("when parsing added to room example event with no cursor") {
        val addedToRoomEvent = UserSubscriptionEventParser(
                testFileReader.readTestFile("added_to_room-withNoCursor.json")
        ).successOrThrow() as AddedToRoomEvent

        it("then result's read states contains valid information") {
            val expectedReadState = RoomReadStateApiType(
                    roomId = "cool-room-2",
                    unreadCount = 15,
                    cursor = null
            )

            assertThat(addedToRoomEvent.readState).isEqualTo(expectedReadState)
        }
    }
})
