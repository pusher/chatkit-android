package com.pusher.chatkit.users

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.TestFileReader
import com.pusher.chatkit.users.UserSubscriptionEvent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UserSubscriptionEventParserSpec : Spek({
    val testFileReader = TestFileReader("/json/subscription/user")

    describe("when parsing initial state example event from the docs") {
        val initialStateEvent = UserSubscriptionEventParser(
                testFileReader.readTestFile("initial_state-docs.json")
        ).successOrThrow() as InitialState

        // TODO: add tests around other bits like unread counts, cursors etc

        it("then result's memberships contain valid information") {
            val memberships = initialStateEvent.memberships

            assertThat(memberships.size).isEqualTo(1)

            assertThat(memberships[0].roomId).isEqualTo("cool-room-1")
            assertThat(memberships[0].userIds.size).isEqualTo(2)
            assertThat(memberships[0].userIds[0]).isEqualTo("jean")
            assertThat(memberships[0].userIds[1]).isEqualTo("ham")
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

    // TODO: add other cases (JSONSs), e.g. for more read states, null cursor, etc

})