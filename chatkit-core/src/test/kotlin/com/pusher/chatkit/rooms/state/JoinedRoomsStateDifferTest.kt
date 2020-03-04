package com.pusher.chatkit.rooms.state

import assertk.assertThat
import assertk.assertions.containsExactly
import com.pusher.chatkit.state.JoinedRoom
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
            unreadCounts =  mapOf(roomOneId to 1))

        describe("when room is joined"){
            val actions = differ.toActions(
                newRooms = listOf(roomOne, roomTwo),
                newUnreadCounts = mapOf(roomOneId to 1, roomTwoId to 2))

            it("then the result contains JoinedRoom action"){
                assertThat(actions).containsExactly(JoinedRoom(room = roomTwo, unreadCount = 2))
            }
        }
    }


})