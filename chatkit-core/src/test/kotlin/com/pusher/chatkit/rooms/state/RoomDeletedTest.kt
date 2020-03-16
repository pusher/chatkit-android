package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.ChatState
import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.State
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class RoomDeletedTest : Spek({

    describe("given two rooms") {
        val givenState = State.initial().with(
            ChatState.initial().with(
                joinedRoomsState = JoinedRoomsState(
                    rooms = mapOf(
                        roomOneId to roomOne,
                        roomTwoId to roomTwo
                    ),
                    unreadCounts = mapOf(
                        roomOneId to 1,
                        roomTwoId to 2
                    )
                )
            )
        )

        describe("when a room is deleted") {
            val newState = roomDeletedReducer(
                givenState,
                RoomDeleted(roomOneId)
            )

            it("then the state contains only the non-deleted room") {
                assertThat(newState.chatState.joinedRoomsState)
                    .isNotNull()
                    .containsOnly(roomTwoId to roomTwo)
            }
        }
    }
})
