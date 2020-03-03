package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.LeftRoom
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class LeftRoomTest : Spek({

    describe("given a joined rooms state of two rooms") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(
                        mapOf(
                                JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne,
                                JoinedRoomsStateTestUtil.roomTwoId to JoinedRoomsStateTestUtil.roomTwo
                        ),
                        mapOf(
                                JoinedRoomsStateTestUtil.roomOneId to 1,
                                JoinedRoomsStateTestUtil.roomTwoId to 2
                        )
                )
        )

        describe("when an event for leaving a room that is part of the state is received") {
            val newState = leftRoomReducer(initialState,
                    LeftRoom(JoinedRoomsStateTestUtil.roomOneId))

            it("then the state should be empty") {
                assertThat(newState.joinedRoomsState).isNotNull()
                        .containsOnly(JoinedRoomsStateTestUtil.roomTwoId
                                to JoinedRoomsStateTestUtil.roomTwo)
            }
        }
    }

})
