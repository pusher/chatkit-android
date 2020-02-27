package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
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
                    UpdatedRoom(JoinedRoomsStateTestUtil.roomOneUpdated, 2))

            it("then the state should contain the updated room") {
                assertThat(updatedState.joinedRoomsState).isNotNull()
                        .containsOnly(JoinedRoomsStateTestUtil.roomOneId
                                to JoinedRoomsStateTestUtil.roomOneUpdated)
            }

            it("then the unread counts should be updated") {
                assertThat(updatedState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        JoinedRoomsStateTestUtil.roomOneId to 2)
            }
        }
    }

    describe("given initial empty state=") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(mapOf(), mapOf()))

        describe("when an event for updating a room that is not a member of the state is received") {
            val updatedState = updatedRoomReducer(initialState,
                    UpdatedRoom(JoinedRoomsStateTestUtil.roomOne, 1))

            it("then the state should contain the updated room") {
                assertThat(updatedState.joinedRoomsState).isNotNull()
                        .containsOnly(JoinedRoomsStateTestUtil.roomOneId
                                to JoinedRoomsStateTestUtil.roomOne)
            }

            it("then the unread counts should be updated") {
                assertThat(updatedState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        JoinedRoomsStateTestUtil.roomOneId to 1)
            }
        }
    }
})
