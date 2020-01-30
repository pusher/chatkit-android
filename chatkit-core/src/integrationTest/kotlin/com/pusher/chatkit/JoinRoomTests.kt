package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.util.FutureValue
import com.pusher.util.Result
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object JoinRoomTests : Spek({
    afterGroup(::tearDownInstance)
    afterGroup(::closeChatManagers)

    describe("currentUser '$PUSHERINO' joins room") {

        setUpInstanceWith(
                createDefaultRole(),
                newUsers(PUSHERINO),
                newRoom(GENERAL))

        val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
        val room = pusherino.joinRoom(GENERAL)

        check(room is Result.Success) { (room as? Result.Failure)?.error as Any }
        assertThat(pusherino.rooms).contains(pusherino.generalRoom)
    }

    describe ("currentUser '$PUSHERINO' joins room with other members") {
        setUpInstanceWith(
                createDefaultRole(),
                newUsers(PUSHERINO, ALICE),
                newRoom(GENERAL, ALICE))

        var pusherinoJoinedRoomEvent by FutureValue<ChatEvent.AddedToRoom>()
        val pusherino = chatFor(PUSHERINO).connect{ event ->
            when (event) {
                is ChatEvent.AddedToRoom -> {
                    pusherinoJoinedRoomEvent = event
                }
            }
        }.assumeSuccess()

        val room = pusherino.joinRoom(GENERAL).assumeSuccess()

        assertThat(room.memberUserIds)
                .containsExactly(ALICE, PUSHERINO, SUPER_USER)
        assertThat(pusherino.rooms[0].memberUserIds)
                .containsExactly(ALICE, PUSHERINO, SUPER_USER)
        assertThat(pusherinoJoinedRoomEvent.room.memberUserIds)
                .containsExactly(ALICE, PUSHERINO, SUPER_USER)
    }

    describe("currentUser '$PUSHERINO' leaves room") {
        setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

        val pusherino = chatFor(SUPER_USER).connect().assumeSuccess()
        val generalRoom = pusherino.generalRoom

        val room = pusherino.leaveRoom(generalRoom)

        check(room is Result.Success) { (room as? Result.Failure)?.error as Any }
        assertThat(pusherino.rooms).doesNotContain(generalRoom)
    }

    describe( "current user '$PUSHERINO") {
        setUpInstanceWith(
                createDefaultRole(),
                newUsers(PUSHERINO, ALICE),
                newRoom(GENERAL, ALICE)
        )

        var addedToRoomEvent by FutureValue<ChatEvent.AddedToRoom>()
        val pusherino = chatFor(PUSHERINO).connect { event ->
            if (event is ChatEvent.AddedToRoom) addedToRoomEvent = event
        }.assumeSuccess()

        it( "has no current rooms") {
            assertThat(pusherino.rooms).isEmpty()
        }

        it ("has one joinable rooms") {
            val joinableRooms = pusherino.getJoinableRooms().assumeSuccess()

            assertThat(joinableRooms.size).isEqualTo(1)
            assertThat(joinableRooms[0].id).isEqualTo(GENERAL)
            assertThat(joinableRooms[0].memberUserIds).isEmpty()
        }

        it ("joins room '$GENERAL' with accurate membership") {
            val generalRoom = pusherino.joinRoom(GENERAL).assumeSuccess()
            assertThat(generalRoom.memberUserIds)
                    .containsExactly(PUSHERINO, ALICE, SUPER_USER)

            assertThat(pusherino.rooms).contains(pusherino.generalRoom)
            assertThat(pusherino.rooms[0].memberUserIds)
                    .containsExactly(PUSHERINO, ALICE, SUPER_USER)

            assertThat(addedToRoomEvent.room.id).isEqualTo(GENERAL)
            assertThat(addedToRoomEvent.room.memberUserIds)
                    .containsExactly(PUSHERINO, ALICE, SUPER_USER)
        }

        it ("leaves room '$GENERAL'") {
            pusherino.leaveRoom(GENERAL).assumeSuccess()
            assertThat(pusherino.rooms).isEmpty()
        }

    }
})
