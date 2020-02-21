package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.AuxiliaryState
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.ReducerLastChange
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomDeletedTest : Spek({

    describe("RoomDeleted") {

        it("with initial state of two rooms") {
            // given
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
            val currentState = ChatkitState(
                    joinedRoomsState,
                    AuxiliaryState(0, ReducerLastChange(0, joinedRoomsState))
            )

            // when
            val roomDeleted = RoomDeleted(
                    RoomsUtil.roomOneId
            )
            val updatedState = roomDeletedReducer(currentState, roomDeleted)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms)
                    .containsOnly(
                            Pair(RoomsUtil.roomTwoId, RoomsUtil.roomTwo)
                    )

            assertThat(updatedState.joinedRoomsState.unreadCounts)
                    .containsOnly(
                            Pair(RoomsUtil.roomTwoId, 2)
                    )
        }

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
            val roomDeleted = RoomDeleted(
                    RoomsUtil.roomOneId
            )
            val updatedState = roomDeletedReducer(currentState, roomDeleted)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms).isEmpty()

            assertThat(updatedState.joinedRoomsState.unreadCounts).isEmpty()
        }

        it("with initial state of null rooms") {
            // given
            val currentState = ChatkitState(
                    null,
                    null
            )

            // when
            val roomDeleted = RoomDeleted(
                    RoomsUtil.roomOneId
            )
            val updatedState = roomDeletedReducer(currentState, roomDeleted)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms).isEmpty()

            assertThat(updatedState.joinedRoomsState.unreadCounts).isEmpty()
        }

        it("with list of rooms not containing deleted room") {
            // given
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
            val currentState = ChatkitState(
                    joinedRoomsState,
                    AuxiliaryState(0, ReducerLastChange(0, joinedRoomsState))
            )

            // when
            val roomDeleted = RoomDeleted(
                    RoomsUtil.roomThreeId
            )
            val updatedState = roomDeletedReducer(currentState, roomDeleted)

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
    }
})
