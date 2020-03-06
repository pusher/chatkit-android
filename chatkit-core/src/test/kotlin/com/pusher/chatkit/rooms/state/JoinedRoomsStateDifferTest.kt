package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsExactly
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.ReconnectJoinedRoom
import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.State
import io.mockk.every
import io.mockk.mockk
import org.reduxkotlin.GetState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomsStateDifferTest : Spek({

    describe("given one room") {
        val state = mockk<GetState<State>>(relaxed = true)
        val differ = JoinedRoomsStateDiffer(state)

        every { state().joinedRoomsState } returns JoinedRoomsState(
            rooms = mapOf(roomOneId to roomOne),
            unreadCounts = mapOf(roomOneId to 1))

        describe("when room is joined") {
            val actions = differ.toActions(
                newRooms = listOf(roomOne, roomTwo),
                newUnreadCounts = mapOf(roomOneId to 1, roomTwoId to 2))

            it("then the result contains JoinedRoom action") {
                assertThat(actions).containsExactly(ReconnectJoinedRoom(room = roomTwo, unreadCount = 2))
            }
        }
    }

    describe("given two rooms") {
        val state = mockk<GetState<State>>(relaxed = true)
        val differ = JoinedRoomsStateDiffer(state)

        every { state().joinedRoomsState } returns JoinedRoomsState(
            rooms = mapOf(roomOneId to roomOne, roomTwoId to roomTwo),
            unreadCounts = mapOf(roomOneId to 1, roomTwoId to 2))

        describe("when a room is left") {
            val actions = differ.toActions(
                newRooms = listOf(roomOne),
                newUnreadCounts = mapOf(roomOneId to 1))

            it("then the result contains RoomUpdated action") {
                assertThat(actions).containsExactly(LeftRoom(roomId = roomTwoId))
            }
        }

        describe("when a room is updated") {
            val actions = differ.toActions(
                newRooms = listOf(roomOneUpdated, roomTwo),
                newUnreadCounts = mapOf(roomOneId to 1, roomTwoId to 2)
            )

            it("then the result contains RoomUpdated action") {
                assertThat(actions).containsExactly(RoomUpdated(room = roomOneUpdated))
            }
        }
    }
})
