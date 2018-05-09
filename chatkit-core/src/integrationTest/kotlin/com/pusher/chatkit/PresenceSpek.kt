package com.pusher.chatkit

import com.google.common.truth.Truth.*
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
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

        it("notifies when user comes online in room") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            var userCameOnline by FutureValue<User>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserCameOnline && event.user.id != PUSHERINO) userCameOnline = event.user
            }

            chatFor(ALICE).connect().wait().assumeSuccess()

            assertThat(userCameOnline.id).isEqualTo(ALICE)
        }

        it("notifies when user comes online globally") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            var userCameOnline by FutureValue<User>()
            chatFor(PUSHERINO).connect{ event ->
                if (event is ChatManagerEvent.UserCameOnline && event.user.id != PUSHERINO) userCameOnline = event.user
            }.wait().assumeSuccess()

            chatFor(ALICE).connect().wait().assumeSuccess()

            assertThat(userCameOnline.id).isEqualTo(ALICE)
        }

        it("notifies when user goes offline in room") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            var userWentOffline by FutureValue<User>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserWentOffline) userWentOffline = event.user
            }

            val aliceChat = chatFor(ALICE)
            aliceChat.connect().wait().assumeSuccess()
            aliceChat.close()

            assertThat(userWentOffline.id).isEqualTo(ALICE)
        }

        it("notifies when user goes offline globally") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            var userWentOffline by FutureValue<User>()
            chatFor(PUSHERINO).connect{ event ->
                if (event is ChatManagerEvent.UserWentOffline) userWentOffline = event.user
            }

            val aliceChat = chatFor(ALICE)
            aliceChat.connect().wait().assumeSuccess()
            aliceChat.close()

            assertThat(userWentOffline.id).isEqualTo(ALICE)
        }

    }

})
