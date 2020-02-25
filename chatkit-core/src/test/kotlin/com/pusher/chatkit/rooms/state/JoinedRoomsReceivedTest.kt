package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomsReceivedTest : Spek({

    describe("given an initial empty state of rooms") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(),
                hashMapOf<String, Int>()
        )

        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when an initial list of joined rooms is received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            JoinedRoomsStateTestUtil.roomOne,
                            JoinedRoomsStateTestUtil.roomTwo
                    ),
                    hashMapOf<String, Int>(
                            Pair(JoinedRoomsStateTestUtil.roomOneId, 1),
                            Pair(JoinedRoomsStateTestUtil.roomTwoId, 2)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            it("then the state should contain two joined rooms and unread counts") {
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

    describe("given a null joined rooms state") {
        val currentState = ChatkitState(joinedRoomsState = null)

        describe("when an initial list of joined rooms is received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            JoinedRoomsStateTestUtil.roomOne,
                            JoinedRoomsStateTestUtil.roomTwo
                    ),
                    hashMapOf<String, Int>(
                            Pair(JoinedRoomsStateTestUtil.roomOneId, 1),
                            Pair(JoinedRoomsStateTestUtil.roomTwoId, 2)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            it("then the state should contain two joined rooms and unread counts") {
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

    describe("given an initial joined room state with one room") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, JoinedRoomsStateTestUtil.roomOne)
                ),
                hashMapOf<String, Int>(
                        Pair(JoinedRoomsStateTestUtil.roomOneId, 1)
                )
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when a new initial list of joined rooms is received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            JoinedRoomsStateTestUtil.roomTwo,
                            JoinedRoomsStateTestUtil.roomThree
                    ),
                    hashMapOf<String, Int>(
                            Pair(JoinedRoomsStateTestUtil.roomTwoId, 2),
                            Pair(JoinedRoomsStateTestUtil.roomThreeId, 3)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            it("then the state should only contain the two new joined rooms") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomTwoId, JoinedRoomsStateTestUtil.roomTwo),
                                Pair(JoinedRoomsStateTestUtil.roomThreeId, JoinedRoomsStateTestUtil.roomThree)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair(JoinedRoomsStateTestUtil.roomTwoId, 2),
                                Pair(JoinedRoomsStateTestUtil.roomThreeId, 3)
                        )
            }
        }
    }
})
