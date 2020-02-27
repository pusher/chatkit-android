package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomTest : Spek({

    describe("given initial state") {
        val initialState = ChatkitState(
                joinedRoomsState = JoinedRoomsState(mapOf(), mapOf()))

        describe("when one new room with unread counts is received") {
            val joinedRoom = JoinedRoom(JoinedRoomsStateTestUtil.roomOne, unreadCount = 1)
            val updatedState = joinedRoomReducer(initialState, joinedRoom)

            it("then the state should contain the expected rooms") {
                assertThat(updatedState.joinedRoomsState).isNotNull().containsOnly(
                        JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne
                )
            }

            it("then the state should contain the expected unread counts") {
                assertThat(updatedState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        JoinedRoomsStateTestUtil.roomOneId to 1
                )
            }
        }
    }
})
