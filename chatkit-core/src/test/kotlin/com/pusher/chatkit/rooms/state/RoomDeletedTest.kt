package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.DeletedRoom
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomDeletedTest : Spek({

    describe("given initial state of one room") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(
                        mapOf(JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne),
                        mapOf(JoinedRoomsStateTestUtil.roomOneId to 1)))

        describe("when an event for deleting a room that is part of the state is received") {
            val updatedState = roomDeletedReducer(initialState,
                    DeletedRoom(JoinedRoomsStateTestUtil.roomOneId))

            it("then the state should be empty") {
                assertThat(updatedState.joinedRoomsState).isNotNull().isEmpty()
            }
        }
    }

    describe("given initial empty state=") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(mapOf(), mapOf()))

        describe("when an event for deleting a room that is not a member of the state is received") {
            val updatedState = roomDeletedReducer(initialState,
                    DeletedRoom(JoinedRoomsStateTestUtil.roomOneId))

            it("then the state should be empty") {
                assertThat(updatedState.joinedRoomsState).isNotNull().isEmpty()
            }
        }
    }
})
