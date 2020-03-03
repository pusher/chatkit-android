package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.RoomDeleted
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomDeletedTest : Spek({

    describe("given a joined rooms state of two rooms") {
        val givenState = ChatkitState(
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

        describe("when an event for deleting a room that is part of the state is received") {
            val newState = roomDeletedReducer(givenState,
                    RoomDeleted(roomOneId))

            it("then the state contains the remaining room") {
                assertThat(newState.joinedRoomsState).isNotNull()
                        .containsOnly(roomTwoId
                                to roomTwo)
            }
        }
    }
})
