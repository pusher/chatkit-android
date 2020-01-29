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
import com.pusher.chatkit.rooms.RoomPushNotificationTitle
import com.pusher.chatkit.test.InstanceActions.changeRoomName
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.deleteRoom
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.test.run
import com.pusher.chatkit.users.User
import com.pusher.chatkit.util.FutureValue
import com.pusher.util.Result.Failure
import com.pusher.util.Result.Success
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object RoomSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Room subscription") {
        it("notifies when '$PUSHERINO' joins room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))

            var userJoined by FutureValue<User>()

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoomMultipart(alice.generalRoom) { event ->
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

            alice.subscribeToRoomMultipart(alice.generalRoom) { event ->
                if (event is RoomEvent.UserLeft) userLeft = event.user
            }
            pusherino.leaveRoom(alice.generalRoom.id).assumeSuccess()

            assertThat(userLeft.id).isEqualTo(pusherino.id)
        }

        it("notifies '$ALICE' when room '$GENERAL' is updated") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            var updatedRoom by FutureValue<Room>()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoomMultipart(alice.generalRoom) { event ->
                if (event is RoomEvent.RoomUpdated) updatedRoom = event.room
            }
            changeRoomName(alice.generalRoom, NOT_GENERAL).run()

            assertThat(updatedRoom.name).isEqualTo(NOT_GENERAL)
        }

        it("notifies '$ALICE' when room '$GENERAL' receives a new message") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(ALICE, PUSHERINO),
                    newRoom(GENERAL, ALICE, PUSHERINO))

            val alice = chatFor(ALICE).connect().assumeSuccess()
            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()


            var lastMessageAtRoomUpdatedEvent by FutureValue<RoomEvent.RoomUpdated>()
            var unreadCountRoomUpdatedEvent by FutureValue<RoomEvent.RoomUpdated>()
            alice.subscribeToRoomMultipart(alice.generalRoom) { event ->
                if (event is RoomEvent.RoomUpdated) {
                    if (event.room.unreadCount == 1) {
                        unreadCountRoomUpdatedEvent = event
                    } else {
                        lastMessageAtRoomUpdatedEvent = event
                    }
                }
            }

            pusherino.sendSimpleMessage(pusherino.generalRoom, "hi")

            assertThat(lastMessageAtRoomUpdatedEvent.room.lastMessageAt).isNotEmpty()
            assertThat(unreadCountRoomUpdatedEvent.room.unreadCount).isEqualTo(1)

            assertThat(alice.rooms[0].unreadCount).isEqualTo(1)
            assertThat(alice.rooms[0].lastMessageAt).isEqualTo(
                    lastMessageAtRoomUpdatedEvent.room.lastMessageAt)
        }

        it("notifies '$PUSHERINO' when room '$GENERAL' is deleted") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var deletedRoomId by FutureValue<String>()
            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val expectedRoomId = pusherino.generalRoom.id

            pusherino.subscribeToRoomMultipart(pusherino.generalRoom) { event ->
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

        it("does not emit any events before the supporting entities are ready") {
            // NB: This test covers a race condition, it may never fail consistently, but flakey
            // behaviour must always be investigated in full.

            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val message1Id = pusherino.sendSimpleMessage(pusherino.generalRoom, "Message 1").assumeSuccess()
            pusherino.sendSimpleMessage(pusherino.generalRoom, "Message 2").assumeSuccess()
            pusherino.setReadCursor(pusherino.generalRoom, message1Id).get().assumeSuccess()

            val alice = chatFor(ALICE).connect().assumeSuccess()

            val badEvents = ConcurrentLinkedQueue<RoomEvent>()
            alice.subscribeToRoomMultipart(alice.generalRoom) { event ->
                when (event) {
                    is RoomEvent.MultipartMessage ->
                        if (!alice.generalRoom.memberUserIds.contains(event.message.sender.id)) {
                            badEvents.add(event)
                        }
                    is RoomEvent.NewReadCursor ->
                        if (!alice.generalRoom.memberUserIds.contains(event.cursor.userId)) {
                            badEvents.add(event)
                        }
                }
            }

            assertThat(badEvents).isEmpty()
        }

        it("always has the correct memberUserIds after room data is changed") {

            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))
            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            val roomUpdated = CountDownLatch(1)

            superUser.subscribeToRoomMultipart(superUser.generalRoom) { event ->
                when (event) {
                    is RoomEvent.RoomUpdated -> {
                        assertThat(event.room.memberUserIds.size).isEqualTo(2)
                        roomUpdated.countDown()
                    }
                }
            }

            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(2) //alice and super user
            superUser.updateRoom(superUser.generalRoom, customData = mapOf("key" to "data")).assumeSuccess()
            roomUpdated.await()
            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(2)

        }

        it("always has the correct memberUserIds after message is sent to a room") {

            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, ALICE))
            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            val roomUpdated = CountDownLatch(1)

            superUser.subscribeToRoomMultipart(superUser.generalRoom) { event ->
                when (event) {
                    is RoomEvent.RoomUpdated -> {
                        assertThat(event.room.memberUserIds.size).isEqualTo(2)
                        roomUpdated.countDown()
                    }
                }
            }

            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(2) //alice and super user
            superUser.sendSimpleMessage(superUser.generalRoom, "hello").assumeSuccess()
            roomUpdated.await()
            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(2)
        }

        it("always has the correct memberUserIds after added to room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL))

            var addedEvent by FutureValue<ChatEvent.AddedToRoom>()
            val alice = chatFor(ALICE).connect { event ->
                if (event is ChatEvent.AddedToRoom) addedEvent = event
            }.assumeSuccess()

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            val joinedEvent = superUser.subscribeRoomFor(GENERAL) { event ->
                event.takeIf { it is RoomEvent.UserJoined && it.user.id == ALICE }
            }

            assertThat(alice.rooms.size).isEqualTo(0)
            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(1)
            superUser.addUsersToRoom(superUser.generalRoom.id, listOf(ALICE)).assumeSuccess()
            joinedEvent.get()
            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(2)

            assertThat(alice.rooms.size).isEqualTo(1)
            assertThat(addedEvent.room.memberUserIds.size).isEqualTo(2)
            assertThat(alice.generalRoom.memberUserIds.size).isEqualTo(2)
        }
    }

    describe("currentUser '$PUSHERINO'") {

        it("creates room without a user supplied id") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val room = pusherino.createRoom(name =  GENERAL).assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
            assertThat(room.id).isNotEmpty()
        }

        it("creates room with a user supplied id") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val roomID = "#general"
            val room = pusherino.createRoom(roomID, GENERAL).assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
            assertThat(room.id).isEqualTo(roomID)
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

        it("creates room with pn title override") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val pnTitleOverride = "my app name"

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val room = pusherino.createRoom(
                    id = null,
                    name = GENERAL,
                    pushNotificationTitleOverride = pnTitleOverride
            ).assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
            assertThat(room.pushNotificationTitleOverride).isEqualTo(pnTitleOverride)
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
                    is ChatEvent.RoomUpdated -> event.room
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
                    is ChatEvent.RoomUpdated -> event.room
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

        it("updates room pn title override") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> event.room
                    else -> null
                }
            }

            val pnTitleOverride = RoomPushNotificationTitle.Override("my app name")

            superUser.updateRoom(
                room = superUser.generalRoom,
                pushNotificationTitleOverride = pnTitleOverride
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(false)
            assertThat(updatedRoom.pushNotificationTitleOverride).isEqualTo(pnTitleOverride.title)
        }

        it("updates to remove pn title override") {
            setUpInstanceWith(
                createDefaultRole(),
                newUsers(PUSHERINO, ALICE),
                newRoom(
                    name = GENERAL,
                    pushNotificationTitleOverride = "my app name",
                    userNames = *arrayOf(PUSHERINO, ALICE)
                )
            )

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            val updatedRoom by chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> event.room
                    else -> null
                }
            }

            superUser.updateRoom(
                room = superUser.generalRoom,
                pushNotificationTitleOverride = RoomPushNotificationTitle.NoOverride
            ).assumeSuccess()

            assertThat(updatedRoom.name).isEqualTo(GENERAL)
            assertThat(updatedRoom.isPrivate).isEqualTo(false)
            assertThat(updatedRoom.pushNotificationTitleOverride).isEqualTo(null)
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
                    is ChatEvent.RoomUpdated -> event.room
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
            pusherino.subscribeToRoomMultipart(pusherino.generalRoom) { }

            val users = pusherino.usersForRoom(pusherino.generalRoom).assumeSuccess()

            assertThat(users.map { it.id }).containsExactly(SUPER_USER, PUSHERINO, ALICE)
        }

        it("is subscribed to room") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoomMultipart(alice.generalRoom) { }

            val isSubscribed = alice.isSubscribedToRoom(alice.generalRoom)

            assertThat(isSubscribed).isTrue()
        }

        it("is not subscribed to room after unsubscribe") {
            setUpInstanceWith(createDefaultRole(), newUsers(ALICE), newRoom(GENERAL, ALICE))

            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.subscribeToRoomMultipart(alice.generalRoom) { }.unsubscribe()

            val isSubscribed = alice.isSubscribedToRoom(alice.generalRoom)

            assertThat(isSubscribed).isFalse()
        }

        it("does not emit onUserJoined for members of a room when the subscription is first created") {

            setUpInstanceWith(createDefaultRole(), newUsers(ALICE, PUSHERINO), newRoom(GENERAL, PUSHERINO, SUPER_USER))

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            var roomUpdatedEvent by FutureValue<RoomEvent.RoomUpdated>()

            val userJoined = AtomicBoolean(false)
            var userJoinedEvent by FutureValue<RoomEvent.UserJoined>()

            superUser.subscribeToRoomMultipart(superUser.generalRoom) { event ->
                when (event) {
                    is RoomEvent.RoomUpdated -> roomUpdatedEvent = event
                    is RoomEvent.UserJoined -> {
                        userJoinedEvent = event
                        userJoined.set(true)
                    }
                }
            }

            //do something else to ensure that no user joined events were actually fired
            superUser.updateRoom(superUser.generalRoom, customData = mapOf("key" to "data")).assumeSuccess()

            assertThat(roomUpdatedEvent.room.customData).containsExactly("key", "data")
            assertThat(userJoined.get()).isEqualTo(false)
            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(2)

            //ensure that one new user did join and calls the UserJoined event
            superUser.addUsersToRoom(superUser.generalRoom.id, listOf(ALICE))

            assertThat(userJoinedEvent.user.id).isEqualTo(ALICE)
            assertThat(superUser.generalRoom.memberUserIds.size).isEqualTo(3)
            assertThat(userJoined.get()).isEqualTo(true)
        }

        it("room updated callback should be called after setting a read cursor") {
            setUpInstanceWith(createDefaultRole(),
                    newRoom(GENERAL, SUPER_USER))

            val roomUpdatedCountDownLatch = CountDownLatch(1)
            val readCursorCountdownLatch = CountDownLatch(1)

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()

            superUser.subscribeToRoomMultipart(superUser.generalRoom) { event ->
                when (event) {
                    is RoomEvent.RoomUpdated -> {
                        if (event.room.customData != null) {
                            roomUpdatedCountDownLatch.countDown()
                        }
                    }
                    is RoomEvent.MultipartMessage -> {
                        superUser.setReadCursor(superUser.generalRoom, event.message.id)
                    }
                    is RoomEvent.NewReadCursor -> {
                        readCursorCountdownLatch.countDown()
                    }
                }
            }

            //send a message and also a read cursor
            superUser.sendSimpleMessage(superUser.generalRoom, "hello")
            readCursorCountdownLatch.await(5, TimeUnit.SECONDS)

            //update the room info
            val customDataUpdateTwo = mapOf("author" to "danielle")
            superUser.updateRoom(superUser.generalRoom, superUser.generalRoom.name,
                    customData = customDataUpdateTwo)
            val latchCompleted = roomUpdatedCountDownLatch.await(5, TimeUnit.SECONDS)
            assertThat(latchCompleted).isTrue()
            assertThat(superUser.generalRoom.customData).isEqualTo(customDataUpdateTwo)
        }
    }
})
