package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RemovedFromRoomTest : Spek({

    describe("given an initial state of two joined rooms") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, JoinedRoomsStateTestUtil.roomOne),
                        Pair(JoinedRoomsStateTestUtil.roomTwoId, JoinedRoomsStateTestUtil.roomTwo)
                ),
                hashMapOf<String, Int>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, 1),
                        Pair(JoinedRoomsStateTestUtil.roomTwoId, 2)
                )
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when a removed from room one event is received") {
            val removedFromRoom = LeftRoom(
                    JoinedRoomsStateTestUtil.roomOneId
            )
            val updatedState = leftRoomReducer(currentState, removedFromRoom)

            it("then the joined rooms state should only contain room two") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomTwoId, JoinedRoomsStateTestUtil.roomTwo)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomTwoId, 2)
                        )
            }
        }
    }

    describe("given an initial empty state") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(),
                hashMapOf<String, Int>()
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when a removed from room one event is received") {
            val removedFromRoom = LeftRoom(
                    JoinedRoomsStateTestUtil.roomOneId
            )
            val updatedState = leftRoomReducer(currentState, removedFromRoom)

            it("then the joined rooms state should still be empty") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms).isEmpty()

                assertThat(updatedState.joinedRoomsState.unreadCounts).isEmpty()
            }
        }
    }

    describe("given an initial null state") {
        val currentState = ChatkitState(joinedRoomsState = null)

        describe("when a removed from room one event is received") {
            val removedFromRoom = LeftRoom(
                    JoinedRoomsStateTestUtil.roomOneId
            )
            val updatedState = leftRoomReducer(currentState, removedFromRoom)

            it("then the joined rooms state should still be empty") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms).isEmpty()

                assertThat(updatedState.joinedRoomsState.unreadCounts).isEmpty()
            }
        }
    }

    describe("given an initial state of two joined rooms") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, JoinedRoomsStateTestUtil.roomOne),
                        Pair(JoinedRoomsStateTestUtil.roomTwoId, JoinedRoomsStateTestUtil.roomTwo)
                ),
                hashMapOf<String, Int>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, 1),
                        Pair(JoinedRoomsStateTestUtil.roomTwoId, 2)
                )
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when a removed from room one event is received which does not " +
                "match a room in the current list") {
            val removedFromRoom = LeftRoom(
                    JoinedRoomsStateTestUtil.roomThreeId
            )
            val updatedState = leftRoomReducer(currentState, removedFromRoom)

            it("then the joined rooms state should contain the original two rooms") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomOneId, JoinedRoomsStateTestUtil.roomOne),
                                Pair(JoinedRoomsStateTestUtil.roomTwoId, JoinedRoomsStateTestUtil.roomTwo)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomOneId, 1),
                                Pair(JoinedRoomsStateTestUtil.roomTwoId, 2)
                        )
            }
        }
    }
})
