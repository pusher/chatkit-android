package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.State
import com.pusher.chatkit.state.LeftRoom
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class LeftRoomTest : Spek({

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

        describe("when a room is left") {
            val newState = leftRoomReducer(givenState, LeftRoom(roomOneId))

            it("then the state no longer contains the room") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnly(roomTwoId to roomTwo)
            }
        }
    }
})
