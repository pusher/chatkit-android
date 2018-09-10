package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Rooms.NOT_GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.changeRoomName
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.deleteRoom
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.test.run
import com.pusher.chatkit.users.User
import com.pusher.platform.network.wait
import com.pusher.util.Result.Failure
import com.pusher.util.Result.Success
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class RoomSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Room subscription") {

        it("notifies when '$PUSHERINO' joins room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))

            var userJoined by FutureValue<User>()

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomEvent.UserJoined) userJoined = event.user
            }

            pusherino.joinRoom(alice.generalRoom.id).wait().assumeSuccess()

            assertThat(userJoined.id).isEqualTo(pusherino.id)
        }

        it("notifies when '$PUSHERINO' leaves room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var userLeft by FutureValue<User>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomEvent.UserLeft) userLeft = event.user
            }
            pusherino.leaveRoom(alice.generalRoom.id).wait().assumeSuccess()

            assertThat(userLeft.id).isEqualTo(pusherino.id)
        }

        it("notifies '$ALICE' when room '$GENERAL' is updated") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            var updatedRoom by FutureValue<Room>()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomEvent.RoomUpdated) updatedRoom = event.room
            }
            changeRoomName(alice.generalRoom, NOT_GENERAL).run()

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
        }

        it("notifies '$PUSHERINO' when room '$GENERAL' is deleted") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var deletedRoomId by FutureValue<Int>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val expectedRoomId = pusherino.generalRoom.id

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                if (event is RoomEvent.RoomDeleted) deletedRoomId = event.roomId
            }
            deleteRoom(pusherino.generalRoom).run()

            assertThat(deletedRoomId).isEqualTo(expectedRoomId)
        }

        it("notifies when '$ALICE' is invited to '$GENERAL") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO))

            var roomJoined by FutureValue<Room>()

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            chatFor(ALICE).connect({ event ->
                if (event is ChatManagerEvent.CurrentUserAddedToRoom) roomJoined = event.room
            }).wait().assumeSuccess()
            pusherino.addUsersToRoom(pusherino.generalRoom.id, listOf(ALICE)).wait().assumeSuccess()

            assertThat(roomJoined.id).isEqualTo(pusherino.generalRoom.id)
        }

        it("notifies when '$ALICE' is removed from '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

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
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            val room = pusherino.createRoom(GENERAL).wait().assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
        }

        it("updates room name") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val superUser = chatFor(SUPER_USER).connect().wait().assumeSuccess()

            val room = superUser.updateRoom(superUser.generalRoom, NOT_GENERAL).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
        }

        it("deletes room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val superUser = chatFor(SUPER_USER).connect().wait().assumeSuccess()

            val room = superUser.deleteRoom(superUser.generalRoom).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
        }

        it("joins room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL))

            val pusherino = chatFor(SUPER_USER).connect().wait().assumeSuccess()

            val room = pusherino.joinRoom(pusherino.generalRoom).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).contains(pusherino.generalRoom)
        }

        it("leaves room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val pusherino = chatFor(SUPER_USER).connect().wait().assumeSuccess()
            val generalRoom = pusherino.generalRoom

            val room = pusherino.leaveRoom(generalRoom).wait()

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).doesNotContain(generalRoom)
        }

        it("gets joinable rooms") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            val rooms = pusherino.getJoinableRooms().wait().assumeSuccess()

            assertThat(rooms).hasSize(1)
            check(rooms[0].name == GENERAL) { "Expected to have room $GENERAL" }
        }

        it("gets rooms") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO), newRoom(NOT_GENERAL))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            val rooms = pusherino.rooms

            assertThat(rooms).hasSize(1)
            check(rooms[0].name == GENERAL) { "Expected to have room $GENERAL" }
        }

        it("provides users for a room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            pusherino.subscribeToRoom(pusherino.generalRoom) { }

            val users = pusherino.usersForRoom(pusherino.generalRoom).wait().assumeSuccess()

            assertThat(users.map { it.id }).containsExactly(SUPER_USER, PUSHERINO, ALICE)
        }

        it("is subscribed to room") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { }

            val isSubscribed = alice.isSubscribedToRoom(alice.generalRoom)

            assertThat(isSubscribed).isTrue()
        }

        it("is not subscribed to room after unsubscribe") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { }.unsubscribe()

            val isSubscribed = alice.isSubscribedToRoom(alice.generalRoom)

            assertThat(isSubscribed).isFalse()
        }
    }
})
