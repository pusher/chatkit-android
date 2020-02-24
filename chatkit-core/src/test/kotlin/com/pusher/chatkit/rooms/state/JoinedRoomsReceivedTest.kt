package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomsReceivedTest : Spek({

    val roomOne = JoinedRoomInternalType(
            "id1",
            "room1",
            false,
            1582283111,
            1582283111,
            null,
            null,
            null
    )

    val roomTwo = JoinedRoomInternalType(
            "id2",
            "room2",
            false,
            1582283112,
            1582283112,
            null,
            null,
            null
    )

    val roomThree = JoinedRoomInternalType(
            "id3",
            "room3",
            false,
            1582283113,
            1582283113,
            null,
            null,
            null
    )

    describe("given an initial empty state of rooms") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(),
                hashMapOf<String, Int>()
        )

        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when an initial list of joined rooms is received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            roomOne,
                            roomTwo
                    ),
                    hashMapOf<String, Int>(
                            Pair("id1", 1),
                            Pair("id2", 2)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            it("then the state should contain two joined rooms and unread counts") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair("id1", roomOne),
                                Pair("id2", roomTwo)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair("id1", 1),
                                Pair("id2", 2)
                        )
            }
        }
    }

    describe("given a null joined rooms state") {
        val currentState = ChatkitState(joinedRoomsState = null)

        describe("when an initial list of joined rooms is received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            roomOne,
                            roomTwo
                    ),
                    hashMapOf<String, Int>(
                            Pair("id1", 1),
                            Pair("id2", 2)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            it("then the state should contain two joined rooms and unread counts") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair("id1", roomOne),
                                Pair("id2", roomTwo)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair("id1", 1),
                                Pair("id2", 2)
                        )
            }
        }
    }

    describe("given an initial joined room state with one room") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(
                        Pair("id1", roomOne)
                ),
                hashMapOf<String, Int>(
                        Pair("id1", 1)
                )
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when a new initial list of joined rooms is received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            roomTwo,
                            roomThree
                    ),
                    hashMapOf<String, Int>(
                            Pair("id2", 2),
                            Pair("id3", 3)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            it("then the state should only contain the two new joined rooms") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair("id2", roomTwo),
                                Pair("id3", roomThree)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair("id2", 2),
                                Pair("id3", 3)
                        )
            }
        }
    }
})
