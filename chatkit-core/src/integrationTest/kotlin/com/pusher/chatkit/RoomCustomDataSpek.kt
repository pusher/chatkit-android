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
import junit.framework.Assert.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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

            var superUserRoomUpdated by FutureValue<Room>()
            val superUser = chatFor(SUPER_USER).connect { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        superUserRoomUpdated = event.room
                    }
                }
            }.assumeSuccess()

            val newCustomData = mapOf(
                    "added" to "some",
                    "custom" to "data"
            )

            var alicesUpdatedGeneralRoom by FutureValue<Room>()
            chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        alicesUpdatedGeneralRoom = event.room
                    }
                }
            }

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = newCustomData

            ).assumeSuccess()

            assertThat(alicesUpdatedGeneralRoom.customData).isEqualTo(newCustomData)
            assertThat(superUserRoomUpdated.customData).isEqualTo(newCustomData)
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

            var superUserRoomUpdated by FutureValue<Room>()
            val superUser = chatFor(SUPER_USER).connect { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        superUserRoomUpdated = event.room
                    }
                }
            }.assumeSuccess()

            var alicesUpdatedGeneralRoom by FutureValue<Room>()
            chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        alicesUpdatedGeneralRoom = event.room
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

            assertThat(alicesUpdatedGeneralRoom.customData).isEqualTo(newCustomData)
            assertThat(superUserRoomUpdated.customData).isEqualTo(newCustomData)
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

            var superUserRoomUpdated by FutureValue<Room>()
            val superUser = chatFor(SUPER_USER).connect { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {
                        superUserRoomUpdated = event.room
                    }
                }
            }.assumeSuccess()

            var alicesUpdatedGeneralRoom by FutureValue<Room>()
            chatFor(ALICE).connectFor { event ->
                when (event) {
                    is ChatEvent.RoomUpdated -> {

                        alicesUpdatedGeneralRoom = event.room
                    }
                }
            }

            val emptyCustomData = mapOf<String, String>()

            superUser.updateRoom(
                    room = superUser.generalRoom,
                    customData = emptyCustomData
            ).assumeSuccess()

            assertThat(alicesUpdatedGeneralRoom.customData).isEqualTo(emptyCustomData)
            assertThat(superUserRoomUpdated.customData).isEqualTo(emptyCustomData)
            assertNotNull(superUser.rooms[0].customData)
            assertThat(superUser.rooms[0].customData).isEqualTo(emptyCustomData)
        }

    }
})
