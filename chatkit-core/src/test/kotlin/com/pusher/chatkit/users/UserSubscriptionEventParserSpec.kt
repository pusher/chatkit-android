package com.pusher.chatkit.users

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.users.UserSubscriptionEvent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UserSubscriptionEventParserSpec : Spek({
    describe("when parsing initial state example event from the docs") {
        val initialStateEvent = UserSubscriptionEventParser(readTestJson("initial_state-docs"))
                .successOrThrow() as InitialState

        // TODO: add tests around other bits like unread counts, cursors etc

        it("then result's memberships will contain valid information") {
            val memberships = initialStateEvent.memberships

            assertThat(memberships.size).isEqualTo(2)

            assertThat(memberships[0].roomId).isEqualTo("cool-room-1")
            assertThat(memberships[0].userIds.size).isEqualTo(2)
            assertThat(memberships[0].userIds[0]).isEqualTo("jean")
            assertThat(memberships[0].userIds[1]).isEqualTo("ham")

            assertThat(memberships[1].roomId).isEqualTo("party-room")
            assertThat(memberships[1].userIds.size).isEqualTo(1)
            assertThat(memberships[1].userIds[0]).isEqualTo("ham")
        }

    }
    describe("when parsing added to room example event from the docs") {
        val addedToRoomEvent = UserSubscriptionEventParser(readTestJson("added_to_room-docs"))
                .successOrThrow() as AddedToRoomEvent

        it("then result's room will have unread count relayed from read state") {
            assertThat(addedToRoomEvent.room.unreadCount).isEqualTo(15)
        }
        it("then result's memberships will contain valid information") {
            val membership = addedToRoomEvent.memberships
            assertThat(membership.roomId).isEqualTo("cool-room-2")
            assertThat(membership.userIds.size).isEqualTo(1)
            assertThat(membership.userIds[0]).isEqualTo("ham")
        }

    }
    describe("when parsing user joined room example event from the docs") {
        val userJoinedRoomEvent = UserSubscriptionEventParser(readTestJson("user_joined_room-docs"))
                .successOrThrow() as UserJoinedRoomEvent

        it("then result will have valid user/room IDs") {
            assertThat(userJoinedRoomEvent.userId).isEqualTo("xavier")
            assertThat(userJoinedRoomEvent.roomId).isEqualTo("cool-room-1")
        }
    }
    describe("when parsing user left room example event from the docs") {
        val userJoinedRoomEvent = UserSubscriptionEventParser(readTestJson("user_left_room-docs"))
                .successOrThrow() as UserLeftRoomEvent

        it("then result will have valid user/room IDs") {
            assertThat(userJoinedRoomEvent.userId).isEqualTo("xavier")
            assertThat(userJoinedRoomEvent.roomId).isEqualTo("cool-room-1")
        }
    }

    // TODO: add other cases (JSONSs), e.g. for more read states, null cursor, etc

})