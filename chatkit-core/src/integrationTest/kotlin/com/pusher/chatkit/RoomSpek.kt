package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Rooms.NOT_GENERAL
import com.pusher.chatkit.Rooms.SAMPLE_CUSTOM_DATA
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.util.FutureValue
import com.pusher.chatkit.test.InstanceActions.changeRoomName
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.deleteRoom
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.test.run
import com.pusher.chatkit.users.User
import com.pusher.util.Result.Failure
import com.pusher.util.Result.Success
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class RoomSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Room subscription") {
        it("notifies when '$PUSHERINO' joins room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))

            var userJoined by FutureValue<User>()

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                // TODO this represents a change in behaviour
                // Previously we didn't report users joined and left from the initial state
                if (event is RoomEvent.UserJoined && event.user.id == PUSHERINO) userJoined = event.user
            }

            assertThat(alice.rooms[0].memberUserIds).doesNotContain(PUSHERINO)

            pusherino.joinRoom(alice.generalRoom.id).assumeSuccess()

            assertThat(userJoined.id).isEqualTo(pusherino.id)
        }

        it("notifies when '$PUSHERINO' leaves room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var userLeft by FutureValue<User>()
            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomEvent.UserLeft) userLeft = event.user
            }
            pusherino.leaveRoom(alice.generalRoom.id).assumeSuccess()

            assertThat(userLeft.id).isEqualTo(pusherino.id)
        }

        it("notifies '$ALICE' when room '$GENERAL' is updated") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            var updatedRoom by FutureValue<Room>()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomEvent.RoomUpdated) updatedRoom = event.room
            }
            changeRoomName(alice.generalRoom, NOT_GENERAL).run()

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
        }

        it("notifies '$PUSHERINO' when room '$GENERAL' is deleted") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var deletedRoomId by FutureValue<String>()
            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
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

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            chatFor(ALICE).connect() { event ->
                if (event is ChatEvent.AddedToRoom) roomJoined = event.room
            }.assumeSuccess()
            pusherino.addUsersToRoom(pusherino.generalRoom.id, listOf(ALICE)).assumeSuccess()

            assertThat(roomJoined.id).isEqualTo(pusherino.generalRoom.id)
        }

        it("notifies when '$ALICE' is removed from '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var roomRemovedFromId by FutureValue<String>()
            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            chatFor(ALICE).connect() { event ->
                if (event is ChatEvent.RemovedFromRoom) roomRemovedFromId = event.roomId
            }.assumeSuccess()

            pusherino.removeUsersFromRoom(pusherino.generalRoom.id, listOf(ALICE)).assumeSuccess()

            assertThat(roomRemovedFromId).isEqualTo(pusherino.generalRoom.id)
        }

    }

    describe("currentUser '$PUSHERINO'") {

        it("creates room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val room = pusherino.createRoom(GENERAL).assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
        }

        it("creates private room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val room = pusherino.createRoom(
                    name = GENERAL,
                    isPrivate = true
            ).assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
            assertThat(room.isPrivate).isEqualTo(true)
        }

        it("creates room with custom data") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val customData = mapOf(
                    "this is" to listOf("complex", "data"),
                    "this key" to "has string value"
            )

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val room = pusherino.createRoom(
                    name = GENERAL,
                    customData = customData
            ).assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
            assertThat(room.customData).isEqualTo(customData)
        }

        it("updates room name") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated ->event.room
                    else -> null
                }
            }

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    name = NOT_GENERAL
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(false)
            assertThat(updatedRoom.customData).isEqualTo(SAMPLE_CUSTOM_DATA)
        }

        it("user receives room updated event with new name after reconnecting (room updated when offline)") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            val pusherino = chatFor(PUSHERINO)
            pusherino.connect().assumeSuccess()
            pusherino.close()

            // update the room while pusherino is offline
            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            superUser.updateRoom(
                    room = superUser.generalRoom,
                    name = NOT_GENERAL
            ).assumeSuccess()

            // reconnect
            val updatedRoom by pusherino.connectFor{ event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> event.room
                    else -> null
                }
            }

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
        }

        it("user receives room updated event with new custom data after reconnecting (room updated when offline)") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            val pusherino = chatFor(PUSHERINO)
            pusherino.connect().assumeSuccess()
            pusherino.close()

            val newCustomData = mapOf("foo" to "bar")
            // update the room while pusherino is offline
            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = newCustomData
            ).assumeSuccess()

            // reconnect
            val updatedRoom by pusherino.connectFor{ event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> event.room
                    else -> null
                }
            }

            assertThat(updatedRoom.name).isEqualTo(GENERAL)
            assertThat(updatedRoom.customData).isEqualTo(newCustomData)
        }

        it("updates room privacy") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated ->event.room
                    else -> null
                }
            }

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    isPrivate = true
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(true)
            assertThat(updatedRoom.customData).isEqualTo(SAMPLE_CUSTOM_DATA)
        }

        it("adds room customData") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated ->event.room
                    else -> null
                }
            }

            val newCustomData = mapOf(
                    "added" to "some",
                    "custom" to "data"
            )

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = newCustomData
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(false)
            assertThat(updatedRoom.customData).isEqualTo(newCustomData)
        }

        it("updates existing room customData") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated ->event.room
                    else -> null
                }
            }

            val newCustomData = mapOf(
                    "replaced" to "some",
                    "custom" to "data"
            )

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = newCustomData
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(false)
            assertThat(updatedRoom.customData).isEqualTo(newCustomData)
        }

        it("updates to remove customData") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated ->event.room
                    else -> null
                }
            }

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = mapOf()
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(false)
            assertThat(updatedRoom.customData).isEqualTo(emptyMap<String, Any?>())
        }

        it("updates all the room fields") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated ->event.room
                    else -> null
                }
            }

            val newCustomData = mapOf(
                    "added" to "some",
                    "custom" to "data"
            )

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    name = NOT_GENERAL,
                    isPrivate = true,
                    customData = newCustomData
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(true)
            assertThat(updatedRoom.customData).isEqualTo(newCustomData)
        }

        it("deletes room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val room = superUser.deleteRoom(superUser.generalRoom)

            check(room is Success) { (room as? Failure)?.error as Any }
        }

        it("joins room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL))

            val pusherino = chatFor(SUPER_USER).connect().assumeSuccess()

            val room = pusherino.joinRoom(pusherino.generalRoom)

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).contains(pusherino.generalRoom)
        }

        it("leaves room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val pusherino = chatFor(SUPER_USER).connect().assumeSuccess()
            val generalRoom = pusherino.generalRoom

            val room = pusherino.leaveRoom(generalRoom)

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).doesNotContain(generalRoom)
        }

        it("gets joinable rooms") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val rooms = pusherino.getJoinableRooms().assumeSuccess()

            assertThat(rooms).hasSize(1)
            check(rooms[0].name == GENERAL) { "Expected to have room $GENERAL" }
        }

        it("gets rooms") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO), newRoom(NOT_GENERAL))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val rooms = pusherino.rooms

            assertThat(rooms).hasSize(1)
            check(rooms[0].name == GENERAL) { "Expected to have room $GENERAL" }
        }

        it("provides users for a room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            pusherino.subscribeToRoom(pusherino.generalRoom) { }

            val users = pusherino.usersForRoom(pusherino.generalRoom).assumeSuccess()

            assertThat(users.map { it.id }).containsExactly(SUPER_USER, PUSHERINO, ALICE)
        }

        it("is subscribed to room") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { }

            val isSubscribed = alice.isSubscribedToRoom(alice.generalRoom)

            assertThat(isSubscribed).isTrue()
        }

        it("is not subscribed to room after unsubscribe") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { }.unsubscribe()

            val isSubscribed = alice.isSubscribedToRoom(alice.generalRoom)

            assertThat(isSubscribed).isFalse()
        }
    }
})
