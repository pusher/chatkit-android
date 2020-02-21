package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.AuxiliaryState
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.state.ReducerLastChange
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class AddedToRoomTest : Spek({

    describe("AddedToRoom") {

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
            val addedToRoomRoom = AddedToRoom(
                    RoomsUtil.roomOne,
                    1
            )
            val updatedState = addedToRoomReducer(currentState, addedToRoomRoom)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, RoomsUtil.roomOne)
                    )

            assertThat(updatedState.joinedRoomsState.unreadCounts)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, 1)
                    )
        }

        // it has null rooms
        it("with initial state of null rooms") {
            // given
            val currentState = ChatkitState(
                    null,
                    null
            )

            // when
            val addedToRoomRoom = AddedToRoom(
                    RoomsUtil.roomOne,
                    1
            )
            val updatedState = addedToRoomReducer(currentState, addedToRoomRoom)

            // then
            assertThat(updatedState.joinedRoomsState).isNotNull()

            assertThat(updatedState.joinedRoomsState!!.rooms)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, RoomsUtil.roomOne)
                    )

            assertThat(updatedState.joinedRoomsState.unreadCounts)
                    .containsOnly(
                            Pair(RoomsUtil.roomOneId, 1)
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
            val addedToRoomRoom = AddedToRoom(
                    RoomsUtil.roomTwo,
                    2
            )
            val updatedState = addedToRoomReducer(currentState, addedToRoomRoom)

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
