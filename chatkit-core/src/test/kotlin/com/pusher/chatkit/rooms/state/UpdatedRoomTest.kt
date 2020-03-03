package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.UpdatedRoom
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class UpdatedRoomTest : Spek({

    describe("given initial state of one room") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(
                        mapOf(JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne),
                        mapOf(JoinedRoomsStateTestUtil.roomOneId to 1)))

        describe("when an event for updating a room that is part of the state is received") {
            val updatedState = updatedRoomReducer(initialState,
                    UpdatedRoom(JoinedRoomsStateTestUtil.roomOneUpdated))

            it("then the state should contain the updated room") {
                assertThat(updatedState.joinedRoomsState).isNotNull()
                        .containsOnly(JoinedRoomsStateTestUtil.roomOneId
                                to JoinedRoomsStateTestUtil.roomOneUpdated)
            }
        }
    }

    describe("given initial empty state=") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(mapOf(), mapOf()))

        describe("when an event for updating a room that is not a member of the state is received") {
            val updatedState = updatedRoomReducer(initialState,
                    UpdatedRoom(JoinedRoomsStateTestUtil.roomOne))

            it("then the state should contain the updated room") {
                assertThat(updatedState.joinedRoomsState).isNotNull()
                        .containsOnly(JoinedRoomsStateTestUtil.roomOneId
                                to JoinedRoomsStateTestUtil.roomOne)
            }
        }
    }
})
