package com.pusher.chatkit.users

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.users.UserSubscriptionEvent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UserSubscriptionEventParserSpec : Spek({
    describe("when parsing initial state example event from the docs") {
        val initialStateEvent = UserSubscriptionEventParser(readTestJson("initial_state-docs"))
                .successOrThrow() as InitialState

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
    describe("when parsing initial state with read state missing for the second room " +
            "(simulating the top 1000 rooms limit)") {

        val initialStateEvent = UserSubscriptionEventParser(
                readTestJson("initial_state-readState-limit")).successOrThrow() as InitialState

        it("then the first room contains valid unread count") {
            assertThat(initialStateEvent.rooms[0].unreadCount).isEqualTo(7)
        }
        it("then the second room doesn't contain unread count") {
            assertThat(initialStateEvent.rooms[1].unreadCount).isNull()
        }
        it("then the valid cursor is available for the first room (and only the first room)") {
            assertThat(initialStateEvent.cursors.size).isEqualTo(1)
            assertThat(initialStateEvent.cursors[0]).isEqualTo(
                    Cursor("viv", "cool-room-1", 123654, "2017-04-13T14:10:04Z", 0))
        }
    }

    describe("when parsing added to room example event from the docs") {
        val addedToRoomEvent = UserSubscriptionEventParser(readTestJson("added_to_room-docs"))
                .successOrThrow() as AddedToRoomApiEvent

        it("then result's room has unread count relayed from read state") {
            assertThat(addedToRoomEvent.room.unreadCount).isEqualTo(15)
        }
        it("then result's room member is correct") {
            val members = addedToRoomEvent.room.memberUserIds
            assertThat(members.size).isEqualTo(1)
            assertThat(members.first()).isEqualTo("ham")
        }
    }
    describe("when parsing user joined room example event from the docs") {
        val userJoinedRoomEvent = UserSubscriptionEventParser(readTestJson("user_joined_room-docs"))
                .successOrThrow() as UserJoinedRoomEvent

        it("then result has valid user/room IDs") {
            assertThat(userJoinedRoomEvent.userId).isEqualTo("xavier")
            assertThat(userJoinedRoomEvent.roomId).isEqualTo("cool-room-1")
        }
    }
    describe("when parsing user left room example event from the docs") {
        val userJoinedRoomEvent = UserSubscriptionEventParser(readTestJson("user_left_room-docs"))
                .successOrThrow() as UserLeftRoomEvent

        it("then result has valid user/room IDs") {
            assertThat(userJoinedRoomEvent.userId).isEqualTo("xavier")
            assertThat(userJoinedRoomEvent.roomId).isEqualTo("cool-room-1")
        }
    }

    // TODO: add other cases (JSONSs), e.g. for more read states, null cursor, etc

})