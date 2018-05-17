package com.pusher.chatkit

import com.google.common.truth.Truth.*
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.users.User
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class PresenceSpek : Spek({

    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Chatkit with presence") {

        it("notifies when '$ALICE' comes online in room '$GENERAL'") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userCameOnline by chatFor(PUSHERINO)
                .subscribeRoomFor(GENERAL) { (it as? RoomSubscriptionEvent.UserCameOnline)?.takeIf { it.user.id == ALICE } }

            chatFor(ALICE).connect().wait().assumeSuccess()

            assertThat(userCameOnline.user.id).isEqualTo(ALICE)
        }

        it("notifies when '$ALICE' comes online globally") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userCameOnline by chatFor(PUSHERINO)
                .connectFor { (it as? ChatManagerEvent.UserCameOnline)?.takeIf { it.user.id == ALICE } }

            chatFor(ALICE).connect().wait().assumeSuccess()

            assertThat(userCameOnline.user.id).isEqualTo(ALICE)
        }

        it("notifies when '$ALICE' goes offline in room '$GENERAL'") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userWentOffline by chatFor(PUSHERINO)
                .subscribeRoomFor(GENERAL) { it as? RoomSubscriptionEvent.UserWentOffline }

            val aliceChat = chatFor(ALICE)
            aliceChat.connect().wait().assumeSuccess()
            aliceChat.close()

            assertThat(userWentOffline.user.id).isEqualTo(ALICE)
        }

        it("notifies when '$ALICE' goes offline globally") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userWentOffline by chatFor(PUSHERINO)
                .connectFor { it as? ChatManagerEvent.UserWentOffline }

            val aliceChat = chatFor(ALICE)
            aliceChat.connect().wait().assumeSuccess()
            aliceChat.close()

            assertThat(userWentOffline.user.id).isEqualTo(ALICE)
        }

    }

})
