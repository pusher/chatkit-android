package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
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

    describe("ChatManager") {

        it("sends and receives typing indicator") {
            setUpInstanceWith(newUsers(Users.PUSHERINO, Users.ALICE), newRoom("general", Users.PUSHERINO, Users.ALICE))

            var typingUser by FutureValue<User>()

            val pusherinoChat = chatFor(Users.PUSHERINO)
            val aliceChat = chatFor(Users.ALICE)

            val pusherino = pusherinoChat.connect().wait(forTenSeconds).assumeSuccess()
            val alice = aliceChat.connect().wait(forTenSeconds).assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserStartedTyping) {
                    typingUser = event.user
                }
            }

            pusherino.isTypingIn(pusherino.generalRoom).wait(forTenSeconds).assumeSuccess()

            assertThat(typingUser.id).isEqualTo(pusherino.id)

            pusherinoChat.close()
            aliceChat.close()
        }

    }

})
