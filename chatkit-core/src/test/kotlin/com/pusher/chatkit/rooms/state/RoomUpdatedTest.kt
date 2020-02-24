package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatkitState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomUpdatedTest : Spek({

    describe("given an initial state of two rooms") {
        val joinedRoomsState = JoinedRoomsState(
                hashMapOf<String, JoinedRoomInternalType>(
                        Pair(RoomsUtil.roomOneId, RoomsUtil.roomOne),
                        Pair(RoomsUtil.roomTwoId, RoomsUtil.roomTwo)
                ),
                hashMapOf<String, Int>(
                        Pair(RoomsUtil.roomOneId, 1),
                        Pair(RoomsUtil.roomTwoId, 2)
                )
        )
        val currentState = ChatkitState(joinedRoomsState = joinedRoomsState)

        describe("when an updated room event is received") {
            val roomUpdated = RoomUpdated(
                    RoomsUtil.roomOneUpdated,
                    4
            )
            val updatedState = roomUpdatedReducer(currentState, roomUpdated)

            it("then the joined rooms state should contain the updated values") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms)
                        .containsOnly(
                                Pair(RoomsUtil.roomOneId, RoomsUtil.roomOneUpdated),
                                Pair(RoomsUtil.roomTwoId, RoomsUtil.roomTwo)
                        )

                assertThat(updatedState.joinedRoomsState.unreadCounts)
                        .containsOnly(
                                Pair(RoomsUtil.roomOneId, 4),
                                Pair(RoomsUtil.roomTwoId, 2)
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

        describe("when an updated room event is received") {
            val roomUpdated = RoomUpdated(
                    RoomsUtil.roomOneUpdated,
                    4
            )
            val updatedState = roomUpdatedReducer(currentState, roomUpdated)

            it("then the updated room should be present in the joined room state") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms).containsOnly(
                        Pair(RoomsUtil.roomOneId, RoomsUtil.roomOneUpdated)
                )

                assertThat(updatedState.joinedRoomsState.unreadCounts).containsOnly(
                        Pair(RoomsUtil.roomOneId, 4)
                )
            }
        }
    }

    describe("given an initial null state") {
        val currentState = ChatkitState(joinedRoomsState = null)

        describe("when an updated room event is received") {
            val roomUpdated = RoomUpdated(
                    RoomsUtil.roomOneUpdated,
                    4
            )
            val updatedState = roomUpdatedReducer(currentState, roomUpdated)

            it("then the updated room should be present in the joined room state") {
                assertThat(updatedState.joinedRoomsState).isNotNull()

                assertThat(updatedState.joinedRoomsState!!.rooms).containsOnly(
                        Pair(RoomsUtil.roomOneId, RoomsUtil.roomOneUpdated)
                )

                assertThat(updatedState.joinedRoomsState.unreadCounts).containsOnly(
                        Pair(RoomsUtil.roomOneId, 4)
                )
            }
        }
    }
})
