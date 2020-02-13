package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.users.User
import com.pusher.chatkit.util.FutureValue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class UserTypingSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("SynchronousChatManager") {
        it("sends and receives started typing indicator in room") {
            setUpInstanceWith(createDefaultRole(), newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var startedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(Users.ALICE).connect().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomEvent.UserStartedTyping) startedTypingUser = event.user
            }

            pusherino.isTypingIn(pusherino.generalRoom).assumeSuccess()

            assertThat(startedTypingUser.id).isEqualTo(pusherino.id)
        }

        it("sends and receives stopped typing indicator in room") {
            setUpInstanceWith(createDefaultRole(), newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var stoppedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(Users.ALICE).connect().assumeSuccess()

            alice.subscribeToRoom(alice.generalRoom) { event ->
                if (event is RoomEvent.UserStoppedTyping) stoppedTypingUser = event.user
            }

            pusherino.isTypingIn(pusherino.generalRoom).assumeSuccess()

            assertThat(stoppedTypingUser.id).isEqualTo(pusherino.id)
        }

        it("sends and receives started typing indicator globally") {
            setUpInstanceWith(createDefaultRole(), newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var startedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(Users.ALICE).connect { event ->
                if (event is ChatEvent.UserStartedTyping) startedTypingUser = event.user
            }.assumeSuccess()

            // Even though they are reported globally, you must be subscribed to a room to see
            // who is typing there
            alice.subscribeToRoom(pusherino.generalRoom) {}

            pusherino.isTypingIn(pusherino.generalRoom).assumeSuccess()

            assertThat(startedTypingUser.id).isEqualTo(pusherino.id)
        }

        it("sends and receives stopped typing indicator globally") {
            setUpInstanceWith(createDefaultRole(), newUsers(Users.PUSHERINO, Users.ALICE), newRoom(GENERAL, Users.PUSHERINO, Users.ALICE))

            var stoppedTypingUser by FutureValue<User>()

            val pusherino = chatFor(Users.PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(Users.ALICE).connect { event ->
                if (event is ChatEvent.UserStoppedTyping) stoppedTypingUser = event.user
            }.assumeSuccess()

            // Even though they are reported globally, you must be subscribed to a room to see
            // who is typing there
            alice.subscribeToRoom(pusherino.generalRoom) {}

            pusherino.isTypingIn(pusherino.generalRoom).assumeSuccess()

            assertThat(stoppedTypingUser.id).isEqualTo(pusherino.id)
        }
    }
})
