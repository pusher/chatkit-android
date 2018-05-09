package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
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

class UserTypingSpek : Spek({

    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("ChatManager") {

        it("sends and receives typing indicator") {
            setUpInstanceWith(newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var typingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().wait(forTenSeconds).assumeSuccess()
            val alice = chatFor(Users.ALICE).connect().wait(forTenSeconds).assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserStartedTyping) {
                    typingUser = event.user
                }
            }

            pusherino.isTypingIn(pusherino.generalRoom).wait(forTenSeconds).assumeSuccess()

            assertThat(typingUser.id).isEqualTo(pusherino.id)
        }

    }

})
