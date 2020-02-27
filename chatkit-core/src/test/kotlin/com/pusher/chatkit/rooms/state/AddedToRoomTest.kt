package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class AddedToRoomTest : Spek({

    describe("Given an initial state of empty rooms") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(),
                hashMapOf<String, Int>()
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when a new added to room event is received") {
            val addedToRoomRoom = JoinedRoom(
                    JoinedRoomsStateTestUtil.roomOne,
                    1
            )
            val updatedState = joinedRoomReducer(currentState, addedToRoomRoom)

            it("then the state should contain the new joined room") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomOneId, JoinedRoomsStateTestUtil.roomOne)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomOneId, 1)
                        )
            }
        }
    }

    describe("given an initial null state") {
        val currentState = ChatkitState(joinedRoomsState = null)

        describe("when a new added to room event is received") {
            val addedToRoomRoom = JoinedRoom(
                    JoinedRoomsStateTestUtil.roomOne,
                    1
            )
            val updatedState = joinedRoomReducer(currentState, addedToRoomRoom)

            it("then the state should contain the new joined room") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomOneId, JoinedRoomsStateTestUtil.roomOne)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomOneId, 1)
                        )
            }
        }
    }

    describe("given an initial state with one room") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, JoinedRoomsStateTestUtil.roomOne)
                ),
                hashMapOf<String, Int>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, 1)
                )
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("then the state should contain the new and previously joined rooms") {
            val addedToRoomRoom = JoinedRoom(
                    JoinedRoomsStateTestUtil.roomTwo,
                    2
            )
            val updatedState = joinedRoomReducer(currentState, addedToRoomRoom)

            it("then") {
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
