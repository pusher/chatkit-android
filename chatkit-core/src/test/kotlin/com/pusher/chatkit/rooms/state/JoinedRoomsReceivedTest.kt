package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.AuxiliaryState
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.ReducerLastChange
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

    describe("JoinedRoomsReceived") {

        it("with initial state of empty rooms") {
            // given
            val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(),
                hashMapOf<String, Int>()
            )
            val currentState = ChatkitState(
                    joinedRoomsState,
                    AuxiliaryState(0, ReducerLastChange(0, joinedRoomsState))
            )

            // when
            val joinedRoomsReceived = JoinedRoomsReceived (
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

            // then
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

        it("with initial state of null") {
            // given
            val currentState = ChatkitState(
                    null,
                    null
            )

            // when
            val joinedRoomsReceived = JoinedRoomsReceived (
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

            // then
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

        it("with initial state of one room") {
            // given
            val joinedRoomsState = JoinedRoomsState(
                    hashMapOf<String, JoinedRoomInternalType>(
                            Pair("id1", roomOne)
                    ),
                    hashMapOf<String, Int>(
                            Pair("id1", 1)
                    )
            )
            val currentState = ChatkitState(
                    joinedRoomsState,
                    AuxiliaryState(0, ReducerLastChange(0, joinedRoomsState))
            )

            // when
            val joinedRoomsReceived = JoinedRoomsReceived (
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

            // then
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
})