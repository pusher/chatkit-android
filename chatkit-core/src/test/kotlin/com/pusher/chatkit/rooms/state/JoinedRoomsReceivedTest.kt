package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.isNotNull
import com.pusher.chatkit.state.State
import com.pusher.chatkit.state.JoinedRoomsReceived
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomsReceivedTest : Spek({

    describe("given initial state") {
        val initialState = State.initial()

        describe("when no rooms are received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    rooms = emptyList(),
                    unreadCounts = emptyMap()
            )
            val newState = joinedRoomsReceivedReducer(initialState, joinedRoomsReceived)

            it("then the state will be empty") {
                assertThat(newState.joinedRoomsState).isNotNull().isEmpty()
            }
        }

        describe("when two rooms with unread counts are received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    rooms = listOf(
                            roomOne,
                            roomTwo
                    ),
                    unreadCounts = mapOf(
                            roomOneId to 1,
                            roomTwoId to 2
                    )
            )
            val newState = joinedRoomsReceivedReducer(initialState, joinedRoomsReceived)

            it("then the state contains the expected rooms") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnly(
                        roomOneId to roomOne,
                        roomTwoId to roomTwo
                )
            }

            it("then the state contains the expected unread counts") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        roomOneId to 1,
                        roomTwoId to 2
                )
            }
        }

        describe("when two rooms with one missing unread count are received") {
            val joinedRoomsReceived = JoinedRoomsReceived(
                    rooms = listOf(
                            roomOne,
                            roomTwo
                    ),
                    unreadCounts = mapOf(
                            roomOneId to 1
                    )
            )
            val newState = joinedRoomsReceivedReducer(initialState, joinedRoomsReceived)

            it("then the state contains the expected rooms") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnly(
                        roomOneId to roomOne,
                        roomTwoId to roomTwo
                )
            }

            it("then the state contains the expected unread counts") {
                assertThat(newState.joinedRoomsState).isNotNull().containsOnlyUnreadCounts(
                        roomOneId to 1
                )
            }
        }
    }
})
