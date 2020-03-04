package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.State
import com.pusher.chatkit.state.RoomUpdated
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomUpdatedTest : Spek({

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

        describe("when a room is updated") {
            val newState = roomUpdatedReducer(givenState, RoomUpdated(roomOneUpdated))

            it("then the state contains the updated room") {
                assertThat(newState.joinedRoomsState).isNotNull()
                    .contains(roomOneId to roomOneUpdated)
            }

            it("then the state contains the non-updated room") {
                assertThat(newState.joinedRoomsState).isNotNull()
                    .contains(roomTwoId to roomTwo)
            }
        }
    }
})
