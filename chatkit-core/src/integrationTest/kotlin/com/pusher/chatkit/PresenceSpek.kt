package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class PresenceSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Chatkit with presence") {

        it("notifies when '$ALICE' comes online in room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userCameOnline by chatFor(PUSHERINO)
                .subscribeRoomFor(GENERAL) { (it as? RoomEvent.UserCameOnline)?.takeIf { it.user.id == ALICE } }

            chatFor(ALICE).connect().assumeSuccess()

            assertThat(userCameOnline.user.id).isEqualTo(ALICE)
        }

        it("notifies when '$ALICE' comes online globally") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userCameOnline by chatFor(PUSHERINO)
                .connectFor { (it as? ChatManagerEvent.UserCameOnline)?.takeIf { it.user.id == ALICE } }

            chatFor(ALICE).connect().assumeSuccess()

            assertThat(userCameOnline.user.id).isEqualTo(ALICE)
        }

        it("notifies when '$ALICE' goes offline in room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userWentOffline by chatFor(PUSHERINO)
                .subscribeRoomFor(GENERAL) { it as? RoomEvent.UserWentOffline }

            val aliceChat = chatFor(ALICE)
            aliceChat.connect().assumeSuccess()
            aliceChat.close()

            assertThat(userWentOffline.user.id).isEqualTo(ALICE)
        }

        it("notifies when '$ALICE' goes offline globally") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userWentOffline by chatFor(PUSHERINO)
                .connectFor { it as? ChatManagerEvent.UserWentOffline }

            val aliceChat = chatFor(ALICE)
            aliceChat.connect().assumeSuccess()
            aliceChat.close()

            assertThat(userWentOffline.user.id).isEqualTo(ALICE)
        }

    }

})
