package com.pusher.chatkit

import com.google.common.truth.Truth.*
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Rooms.NOT_GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.changeRoomName
import com.pusher.chatkit.test.InstanceActions.deleteRoom
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.test.run
import com.pusher.chatkit.users.User
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xit

class RoomEventsSpek : Spek({

    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Room") {

        xit("notifies when user joins") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))

            var userJoined by FutureValue<User>()

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserJoined) {
                    userJoined = event.user
                }
            }

            pusherino.joinRoom(alice.generalRoom.id).wait().assumeSuccess()

            assertThat(userJoined.id).isEqualTo(pusherino.id)
        }

        xit("notifies when user leaves") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var userJoined by FutureValue<User>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserLeft) userJoined = event.user
            }
            pusherino.leaveRoom(alice.generalRoom.id).wait().assumeSuccess()

            assertThat(userJoined.id).isEqualTo(pusherino.id)
        }

        it("notifies when room is updated") {
            setUpInstanceWith(newUsers(ALICE), newRoom(GENERAL, ALICE))

            var updatedRoom by FutureValue<Room>()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.RoomUpdated) updatedRoom = event.room
            }
            changeRoomName(alice.generalRoom, NOT_GENERAL).run()

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
        }

        it("notifies when room is deleted") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var deletedRoomId by FutureValue<Int>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val expectedRoomId = pusherino.generalRoom.id

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.RoomDeleted) deletedRoomId = event.roomId
            }
            deleteRoom(pusherino.generalRoom).run()

            assertThat(deletedRoomId).isEqualTo(expectedRoomId)
        }

        it("notifies when current user is invited") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO))

            var roomJoined by FutureValue<Room>()

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            chatFor(ALICE).connect({ event ->
                if (event is ChatManagerEvent.CurrentUserAddedToRoom) roomJoined = event.room
            }).wait().assumeSuccess()
            pusherino.addUsersToRoom(pusherino.generalRoom.id, listOf(ALICE)).wait().assumeSuccess()

            assertThat(roomJoined.id).isEqualTo(pusherino.generalRoom.id)
        }

        it("notifies when current user is removed") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var roomRemovedFromId by FutureValue<Int>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            chatFor(ALICE).connect({ event ->
                if (event is ChatManagerEvent.CurrentUserRemovedFromRoom) roomRemovedFromId = event.roomId
            }).wait().assumeSuccess()

            pusherino.removeUsersFromRoom(pusherino.generalRoom.id, listOf(ALICE)).wait().assumeSuccess()

            assertThat(roomRemovedFromId).isEqualTo(pusherino.generalRoom.id)
        }

    }

})
