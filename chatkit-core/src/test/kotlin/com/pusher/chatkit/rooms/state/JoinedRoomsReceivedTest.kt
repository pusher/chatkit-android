package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.JoinedRoomsReceived
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomsReceivedTest : Spek({

    describe("given initial state") {
        val initialState = ChatkitState.initial()

        describe("when no rooms are received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    emptyList(),
                    emptyMap()
            )
            val newState = joinedRoomsReceivedReducer(initialState, joinedRoomsReceived)

            it("then the state should be empty") {
                assertThat(newState.joinedRoomsState).isNotNull().isEmpty()
            }
        }

        describe("when two rooms with unread counts are received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf(
                            JoinedRoomsStateTestUtil.roomOne,
                            JoinedRoomsStateTestUtil.roomTwo
                    ),
                    mapOf(
                            JoinedRoomsStateTestUtil.roomOneId to 1,
                            JoinedRoomsStateTestUtil.roomTwoId to 2
                    )
            )
            val newState = joinedRoomsReceivedReducer(initialState, joinedRoomsReceived)

            it("then the state should contain the expected rooms") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnly(
                        JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne,
                        JoinedRoomsStateTestUtil.roomTwoId to JoinedRoomsStateTestUtil.roomTwo
                )
            }

            it("then the state should contain the expected unread counts") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        JoinedRoomsStateTestUtil.roomOneId to 1,
                        JoinedRoomsStateTestUtil.roomTwoId to 2
                )
            }
        }

        describe("when two rooms with one missing unread count are received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf(
                            JoinedRoomsStateTestUtil.roomOne,
                            JoinedRoomsStateTestUtil.roomTwo
                    ),
                    mapOf(
                            JoinedRoomsStateTestUtil.roomOneId to 1
                    )
            )
            val newState = joinedRoomsReceivedReducer(initialState, joinedRoomsReceived)

            it("then the state should contain the expected rooms") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnly(
                        JoinedRoomsStateTestUtil.roomOneId to JoinedRoomsStateTestUtil.roomOne,
                        JoinedRoomsStateTestUtil.roomTwoId to JoinedRoomsStateTestUtil.roomTwo
                )
            }

            it("then the state should contain the expected unread counts") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        JoinedRoomsStateTestUtil.roomOneId to 1
                )
            }
        }
    }
})
