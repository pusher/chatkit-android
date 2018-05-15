package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Rooms.NOT_GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.Users.SUPER_USER
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
import com.pusher.util.Result.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xit

class RoomSpek : Spek({

    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Room subscription") {

        it("notifies when '$PUSHERINO' joins room '$GENERAL'") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))

            var userJoined by FutureValue<User>()

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserJoined) userJoined = event.user
            }

            pusherino.joinRoom(alice.generalRoom.id).wait().assumeSuccess()

            assertThat(userJoined.id).isEqualTo(pusherino.id)
        }

        it("notifies when '$PUSHERINO' leaves room '$GENERAL'") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var userLeft by FutureValue<User>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserLeft) userLeft = event.user
            }
            pusherino.leaveRoom(alice.generalRoom.id).wait().assumeSuccess()

            assertThat(userLeft.id).isEqualTo(pusherino.id)
        }

        it("notifies '$ALICE' when room '$GENERAL' is updated") {
            setUpInstanceWith(newUsers(ALICE), newRoom(GENERAL, ALICE))

            var updatedRoom by FutureValue<Room>()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.RoomUpdated) updatedRoom = event.room
            }
            changeRoomName(alice.generalRoom, NOT_GENERAL).run()

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
        }

        it("notifies '$PUSHERINO' when room '$GENERAL' is deleted") {
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

        it("notifies when '$ALICE' is invited to '$GENERAL") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO))

            var roomJoined by FutureValue<Room>()

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            chatFor(ALICE).connect({ event ->
                if (event is ChatManagerEvent.CurrentUserAddedToRoom) roomJoined = event.room
            }).wait().assumeSuccess()
            pusherino.addUsersToRoom(pusherino.generalRoom.id, listOf(ALICE)).wait().assumeSuccess()

            assertThat(roomJoined.id).isEqualTo(pusherino.generalRoom.id)
        }

        it("notifies when '$ALICE' is removed from '$GENERAL'") {
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

    describe("currentUser '$PUSHERINO'") {

        it("creates room") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            val room = pusherino.createRoom(GENERAL).wait().assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
        }

        it("updates room name") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val superUser = chatFor(SUPER_USER).connect().wait().assumeSuccess()

            val room = superUser.updateRoom(superUser.generalRoom, NOT_GENERAL).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
        }

        it("deletes room") {
            setUpInstanceWith(newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val superUser = chatFor(SUPER_USER).connect().wait().assumeSuccess()

            val room = superUser.deleteRoom(superUser.generalRoom).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
        }

        it("joins room") {
            setUpInstanceWith(newUsers(PUSHERINO), newRoom(GENERAL))

            val pusherino = chatFor(SUPER_USER).connect().wait().assumeSuccess()

            val room = pusherino.joinRoom(pusherino.generalRoom).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).contains(pusherino.generalRoom)
        }

        it("leaves room") {
            setUpInstanceWith(newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val pusherino = chatFor(SUPER_USER).connect().wait().assumeSuccess()
            val generalRoom = pusherino.generalRoom

            val room = pusherino.leaveRoom(generalRoom).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).doesNotContain(generalRoom)
        }

    }

})
