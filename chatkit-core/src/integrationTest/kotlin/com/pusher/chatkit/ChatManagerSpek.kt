package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.rooms.RoomSubscriptionListeners
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.platform.network.wait
import com.pusher.util.Result
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import elements.Error as ElementsError

class ChatManagerSpek : Spek({

    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("ChatManager with valid instance") {

        it("loads current user") {
            setUpInstanceWith(newUser(PUSHERINO))

            val user = chatFor(PUSHERINO).connect().wait(forTenSeconds)
            val userId = user.assumeSuccess().id

            assertThat(userId).isEqualTo(PUSHERINO)
        }

        it("loads user rooms") {
            setUpInstanceWith(newUser(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val user = chatFor(PUSHERINO).connect().wait(forTenSeconds)
            val roomNames = user.assumeSuccess().rooms.map { it.name }

            assertThat(roomNames).containsExactly(GENERAL)
        }

        it("loads users related to current user") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val user = chatFor(PUSHERINO).connect().wait(forTenSeconds)
            val users = user.assumeSuccess().users.wait(forTenSeconds)

            val relatedUserIds = users.recover { emptyList() }.map { it.id }

            assertThat(relatedUserIds).containsAllOf(ALICE, PUSHERINO)
        }

        it("subscribes to a room and receives message from alice") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait(forTenSeconds)
            val alice = chatFor(ALICE).connect().wait(forTenSeconds)

            val room = pusherino.assumeSuccess().generalRoom

            var messageReceived by FutureValue<Message>()

            pusherino.assumeSuccess().subscribeToRoom(room, RoomSubscriptionListeners(
                onNewMessage = { message -> messageReceived = message },
                onErrorOccurred = { e -> error("error: $e") }
            ))

            val messageResult = alice.assumeSuccess().sendMessage(room, "message text").wait(forTenSeconds)

            check(messageResult is Result.Success)
            assertThat(messageReceived.text).isEqualTo("message text")
        }

    }

})

