package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.State
import com.pusher.chatkit.state.UnreadCountReceived
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UnreadCountReceivedReducerTest : Spek({

    describe("given two rooms") {
        val givenState = State(
            joinedRoomsState = JoinedRoomsState(
                rooms = mapOf(
                    roomOneId to roomOne,
                    roomTwoId to roomTwo
                ),
                unreadCounts = mapOf(
                    roomOneId to 1,
                    roomTwoId to 2
                )
            )
        )

        describe("when an unread count is received") {
            val newState = unreadCountReceivedReducer(givenState,
                UnreadCountReceived(roomOneId, 3))

            it("then the unread count is updated") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                    roomOneId to 3,
                    roomTwoId to 2
                )
            }
        }
    }
})
