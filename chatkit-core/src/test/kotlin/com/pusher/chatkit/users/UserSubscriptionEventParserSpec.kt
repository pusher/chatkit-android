package com.pusher.chatkit.users

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.users.UserSubscriptionEvent.AddedToRoomEvent
import com.pusher.chatkit.users.UserSubscriptionEvent.InitialState
import com.pusher.util.Result
import elements.Error
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UserSubscriptionEventParserSpec : Spek({
    describe("when parsing initial state example event from the docs") {
        lateinit var result : Result<UserSubscriptionEvent, Error>

        beforeEachTest {
            result = UserSubscriptionEventParser(readTestJson("initial_state-docs"))
        }

        // TODO: add tests around other bits like unread counts, cursors etc

        it("then result's memberships will contain valid information") {
            val initialStateEvent = result.successOrThrow() as InitialState

            val membership = initialStateEvent.memberships

            assertThat(membership.size).isEqualTo(2)

            assertThat(membership[0].roomId).isEqualTo("cool-room-1")
            assertThat(membership[0].userIds.size).isEqualTo(2)
            assertThat(membership[0].userIds[0]).isEqualTo("jean")
            assertThat(membership[0].userIds[1]).isEqualTo("ham")

            assertThat(membership[1].roomId).isEqualTo("party-room")
            assertThat(membership[1].userIds.size).isEqualTo(1)
            assertThat(membership[1].userIds[0]).isEqualTo("ham")
        }

    }
    describe("when parsing added to room example event from the docs") {
        lateinit var result : Result<UserSubscriptionEvent, Error>

        beforeEachTest {
            result = UserSubscriptionEventParser(readTestJson("added_to_room-docs"))
        }

        it("then result's room will have unread count relayed from read state") {
            val addedToRoomEvent = result.successOrThrow() as AddedToRoomEvent
            assertThat(addedToRoomEvent.room.unreadCount).isEqualTo(15)
        }

    }

    // TODO: add other cases (JSONSs), e.g. for more read states, null cursor, etc

})