package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
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
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import com.pusher.util.Result
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.TimeUnit.SECONDS
import elements.Error as ElementsError

class ChatManagerSpek : Spek({

    afterEachTest(::tearDownInstance)

    describe("ChatManager with valid instance") {

        it("loads current user") {
            setUpInstanceWith(newUser(PUSHERINO))
            val chat = chatFor(PUSHERINO)

            val user = chat.connect().wait(forTenSeconds)
            val userId = user.assumeSuccess().id

            assertThat(userId).isEqualTo(PUSHERINO)
            chat.close()
        }

        it("loads user rooms") {
            setUpInstanceWith(newUser(PUSHERINO), newRoom("general", PUSHERINO))
            val chat = chatFor(PUSHERINO)

            val user = chat.connect().wait(forTenSeconds)
            val roomNames = user.assumeSuccess().rooms.map { it.name }

            assertThat(roomNames).containsExactly("general")
            chat.close()
        }

        it("loads users related to current user") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom("general", PUSHERINO, ALICE))
            val chat = chatFor(PUSHERINO)

            val user = chat.connect().wait(forTenSeconds)
            val users = user.assumeSuccess().users.wait(forTenSeconds)

            val relatedUserIds = users.recover { emptyList() }.map { it.id }

            assertThat(relatedUserIds).containsAllOf("alice", PUSHERINO)
            chat.close()
        }

        it("subscribes to a room and receives message from alice") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom("general", PUSHERINO, ALICE))
            val chat = chatFor(PUSHERINO)
            val aliceChat = chatFor(ALICE)

            val pusherino = chat.connect().wait(forTenSeconds)
            val alice = aliceChat.connect().wait(forTenSeconds)

            val room = pusherino.assumeSuccess().generalRoom

            var messageReceived by FutureValue<Message>()

            pusherino.assumeSuccess().subscribeToRoom(room, RoomSubscriptionListeners(
                onNewMessage = { message -> messageReceived = message },
                onErrorOccurred = { e -> error("error: $e") }
            ))

            val messageResult = alice.assumeSuccess().sendMessage(room, "message text").wait(forTenSeconds)

            check(messageResult is Result.Success)
            assertThat(messageReceived.text).isEqualTo("message text")
            chat.close()
            aliceChat.close()
        }

    }

})

