package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.util.FutureValue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object JoinRoomTest : Spek({

    lateinit var pusherino: SynchronousCurrentUser
    lateinit var joinRoomResult: Room
    val addedToRoomEvent = FutureValue<ChatEvent.AddedToRoom>()
    beforeEachGroup {
        setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))
        pusherino = chatFor(PUSHERINO).connect{ event ->
            if (event is ChatEvent.AddedToRoom) addedToRoomEvent.set(event)
        }.assumeSuccess()
    }

    afterEachGroup(::closeChatManagers)
    afterEachGroup(::tearDownInstance)

    describe("given no joined rooms and one joinable room") {
        describe("when joining the joinable room knowing its id") {
            beforeGroup {
                joinRoomResult = pusherino.joinRoom(GENERAL).assumeSuccess()
            }

            it("then the result is the expected room") {
                assertMatchesExpectedRoom(joinRoomResult)
            }
            it("then the joined room is stored") {
                assertThat(pusherino.rooms.size).isEqualTo(1)
                assertMatchesExpectedRoom(pusherino.rooms[0])
            }
            it("then expected AddedToRoom is notified") {
                assertMatchesExpectedRoom(addedToRoomEvent.get().room)
            }
        }
        describe("when fetching joinable rooms and joining the fetched room") {
            lateinit var joinableRooms: List<Room>
            beforeGroup {
                joinableRooms = pusherino.getJoinableRooms().assumeSuccess()
                joinRoomResult = pusherino.joinRoom(joinableRooms[0]).assumeSuccess()
            }

            it("then the result is the expected room") {
                assertMatchesExpectedRoom(joinRoomResult)
            }
            it("then the joined room is stored") {
                assertThat(pusherino.rooms.size).isEqualTo(1)
                assertMatchesExpectedRoom(pusherino.rooms[0])
            }
            it("then expected AddedToRoom is notified") {
                assertMatchesExpectedRoom(addedToRoomEvent.get().room)
            }
        }
    }

})

private fun assertMatchesExpectedRoom(room: Room) {
    assertThat(room.id).isEqualTo(GENERAL)
    assertThat(room.memberUserIds).containsExactly(ALICE, PUSHERINO, SUPER_USER)
}
