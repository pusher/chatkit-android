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
import com.pusher.util.Result.Failure
import com.pusher.util.Result.Success
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object JoinRoomTests : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("currentUser '$PUSHERINO'") {

        it("joins room") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO),
                    newRoom(GENERAL))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val room = pusherino.joinRoom(GENERAL)

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).contains(pusherino.generalRoom)
        }

        it ("joins room with other members") {
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

        it("leaves room") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val pusherino = chatFor(SUPER_USER).connect().assumeSuccess()
            val generalRoom = pusherino.generalRoom

            val room = pusherino.leaveRoom(generalRoom)

            check(room is Success) { (room as? Failure)?.error as Any }
            assertThat(pusherino.rooms).doesNotContain(generalRoom)
        }


    }
})
