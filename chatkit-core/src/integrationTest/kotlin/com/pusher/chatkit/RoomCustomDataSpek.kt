package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Rooms.SAMPLE_CUSTOM_DATA
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.util.FutureValue
import junit.framework.Assert.assertNotNull
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object RoomCustomDataSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("room custom data") {

        it("creates a room with custom data") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE))

            val customData = mapOf(
                    "this is" to listOf("complex", "data"),
                    "this key" to "has string value"
            )

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val room = pusherino.createRoom(
                    id = null,
                    name = GENERAL,
                    customData = customData
            ).assumeSuccess()

            assertThat(room.name).isEqualTo(GENERAL)
            assertThat(room.customData).isEqualTo(customData)
        }

        it("adds customData to an existing room") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(GENERAL, PUSHERINO, ALICE)
            )

            var superUserRoomUpdatedEvent by FutureValue<ChatEvent.RoomUpdated>()
            val superUser = chatFor(SUPER_USER).connect { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        superUserRoomUpdatedEvent = event
                    }
                }
            }.assumeSuccess()

            val newCustomData = mapOf(
                    "added" to "some",
                    "custom" to "data"
            )

            var alicesRoomUpdatedEvent by FutureValue<ChatEvent.RoomUpdated>()
            chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        alicesRoomUpdatedEvent = event
                    }
                }
            }

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = newCustomData

            ).assumeSuccess()

            assertThat(alicesRoomUpdatedEvent.room.customData).isEqualTo(newCustomData)
            assertThat(superUserRoomUpdatedEvent.room.customData).isEqualTo(newCustomData)
            assertNotNull(superUser.rooms[0].customData)
            assertThat(superUser.rooms[0].customData).isEqualTo(newCustomData)
        }

        it("updates existing customData to a room") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            var superUserRoomUpdatedEvent by FutureValue<ChatEvent.RoomUpdated>()
            val superUser = chatFor(SUPER_USER).connect { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        superUserRoomUpdatedEvent = event
                    }
                }
            }.assumeSuccess()

            var alicesRoomUpdatedEvent by FutureValue<ChatEvent.RoomUpdated>()
            chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        alicesRoomUpdatedEvent = event
                    }
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

            assertThat(alicesRoomUpdatedEvent.room.customData).isEqualTo(newCustomData)
            assertThat(superUserRoomUpdatedEvent.room.customData).isEqualTo(newCustomData)
            assertNotNull(superUser.rooms[0].customData)
            assertThat(superUser.rooms[0].customData).isEqualTo(newCustomData)
        }

        it("updates customData to an empty map") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(
                            name = GENERAL,
                            customData = SAMPLE_CUSTOM_DATA,
                            userNames = *arrayOf(PUSHERINO, ALICE)
                    )
            )

            var superUserRoomUpdatedEvent by FutureValue<ChatEvent.RoomUpdated>()
            val superUser = chatFor(SUPER_USER).connect { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        superUserRoomUpdatedEvent = event
                    }
                }
            }.assumeSuccess()

            var alicesRoomUpdatedEvent by FutureValue<ChatEvent.RoomUpdated>()
            chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {

                        alicesRoomUpdatedEvent = event
                    }
                }
            }

            val emptyCustomData = mapOf<String, String>()

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = emptyCustomData
            ).assumeSuccess()

            assertThat(alicesRoomUpdatedEvent.room.customData).isEqualTo(emptyCustomData)
            assertThat(superUserRoomUpdatedEvent.room.customData).isEqualTo(emptyCustomData)
            assertNotNull(superUser.rooms[0].customData)
            assertThat(superUser.rooms[0].customData).isEqualTo(emptyCustomData)
        }

    }
})
