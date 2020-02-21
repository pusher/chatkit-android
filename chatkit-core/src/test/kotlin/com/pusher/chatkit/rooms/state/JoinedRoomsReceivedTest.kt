package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.AuxiliaryState
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.ReducerLastChange
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomsReceivedTest : Spek({

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
            val joinedRoomsReceived = JoinedRoomsReceived(
                listOf<JoinedRoomInternalType>(
                        RoomsUtil.roomOne,
                        RoomsUtil.roomTwo
                ),
                hashMapOf<String, Int>(
                        Pair(RoomsUtil.roomOneId, 1),
                        Pair(RoomsUtil.roomTwoId, 2)
                )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, RoomsUtil.roomOne),
                            Pair(RoomsUtil.roomTwoId, RoomsUtil.roomTwo)
                    )

            assertThat(updatedState.joinedRoomsState.unreadCounts)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, 1),
                            Pair(RoomsUtil.roomTwoId, 2)
                    )
        }

        it("with initial state of null") {
            // given
            val currentState = ChatkitState(
                    null,
                    null
            )

            // when
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            RoomsUtil.roomOne,
                            RoomsUtil.roomTwo
                    ),
                    hashMapOf<String, Int>(
                            Pair(RoomsUtil.roomOneId, 1),
                            Pair("id2", 2)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, RoomsUtil.roomOne),
                            Pair(RoomsUtil.roomTwoId, RoomsUtil.roomTwo)
                    )

            assertThat(updatedState.joinedRoomsState.unreadCounts)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, 1),
                            Pair(RoomsUtil.roomTwoId, 2)
                    )
        }

        it("with initial state of one room") {
            // given
            val joinedRoomsState = JoinedRoomsState(
                    hashMapOf<String, JoinedRoomInternalType>(
                            Pair(RoomsUtil.roomOneId, RoomsUtil.roomOne)
                    ),
                    hashMapOf<String, Int>(
                            Pair(RoomsUtil.roomOneId, 1)
                    )
            )
            val currentState = ChatkitState(
                    joinedRoomsState,
                    AuxiliaryState(0, ReducerLastChange(0, joinedRoomsState))
            )

            // when
            val joinedRoomsReceived = JoinedRoomsReceived(
                    listOf<JoinedRoomInternalType>(
                            RoomsUtil.roomTwo,
                            RoomsUtil.roomThree
                    ),
                    hashMapOf<String, Int>(
                            Pair(RoomsUtil.roomTwoId, 2),
                            Pair(RoomsUtil.roomThreeId, 3)
                    )
            )
            val updatedState = joinedRoomsReceivedReducer(currentState, joinedRoomsReceived)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms)
                    .containsOnly(
                            Pair(RoomsUtil.roomTwoId, RoomsUtil.roomTwo),
                            Pair(RoomsUtil.roomThreeId, RoomsUtil.roomThree)
                    )

            assertThat(updatedState.joinedRoomsState.unreadCounts)
                    .containsOnly(
                            Pair(RoomsUtil.roomTwoId, 2),
                            Pair(RoomsUtil.roomThreeId, 3)
                    )
        }
    }
})
