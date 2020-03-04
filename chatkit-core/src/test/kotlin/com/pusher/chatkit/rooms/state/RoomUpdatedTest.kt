package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.RoomUpdated
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomUpdatedTest : Spek({

    describe("given a joined room state with one room") {
        val givenState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(
                        rooms = mapOf(roomOneId to roomOne),
                        unreadCounts = mapOf(roomOneId to 1)))

        describe("when a room is updated") {
            val newState = roomUpdatedReducer(givenState, RoomUpdated(roomOneUpdated))

            it("then the state contains the updated room") {
                assertThat(newState.joinedRoomsState).isNotNull()
                        .containsOnly(roomOneId
                                to roomOneUpdated)
            }
        }
    }
})
