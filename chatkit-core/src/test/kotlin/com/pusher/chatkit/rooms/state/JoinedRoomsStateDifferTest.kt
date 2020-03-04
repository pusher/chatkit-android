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

    describe("given a current state of one room") {
        val state = mockk<GetState<State>>(relaxed = true)
        val differ = JoinedRoomsStateDiffer(state)

        every { state().joinedRoomsState } returns JoinedRoomsState(
            rooms = mapOf(roomOneId to roomOne),
            unreadCounts =  mapOf(roomOneId to 1))
        
        describe("when one new room is added"){
            val actions = differ.toActions(
                newRooms = listOf(roomTwo),
                newUnreadCounts = mapOf(roomTwoId to 2))

            it("then the actions list will contain JoinedRoom"){
                assertThat(actions).containsExactly(JoinedRoom(room = roomTwo, unreadCount = 2))
            }
        }
    }


})