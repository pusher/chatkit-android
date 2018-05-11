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

        it("sends and receives started typing indicator in room") {
            setUpInstanceWith(newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var startedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(Users.ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserStartedTyping) startedTypingUser = event.user
            }

            pusherino.isTypingIn(pusherino.generalRoom).wait().assumeSuccess()

            assertThat(startedTypingUser.id).isEqualTo(pusherino.id)
        }

        it("sends and receives stopped typing indicator in room") {
            setUpInstanceWith(newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var stoppedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(Users.ALICE).connect().wait().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.UserStoppedTyping) stoppedTypingUser = event.user
            }

            pusherino.isTypingIn(pusherino.generalRoom).wait().assumeSuccess()

            assertThat(stoppedTypingUser.id).isEqualTo(pusherino.id)
        }

        it("sends and receives started typing indicator globally") {
            setUpInstanceWith(newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var startedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().wait().assumeSuccess()
            chatFor(Users.ALICE).connect { event ->
                if (event is ChatManagerEvent.UserStartedTyping) startedTypingUser = event.user
            }

            pusherino.isTypingIn(pusherino.generalRoom).wait().assumeSuccess()

            assertThat(startedTypingUser.id).isEqualTo(pusherino.id)
        }

        it("sends and receives stopped typing indicator globally") {
            setUpInstanceWith(newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var stoppedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().wait().assumeSuccess()
            chatFor(Users.ALICE).connect { event ->
                if (event is ChatManagerEvent.UserStoppedTyping) stoppedTypingUser = event.user
            }

            pusherino.isTypingIn(pusherino.generalRoom).wait().assumeSuccess()

            assertThat(stoppedTypingUser.id).isEqualTo(pusherino.id)
        }

    }

})
