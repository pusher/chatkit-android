package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.platform.network.Wait
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.wait
import com.pusher.util.Result
import elements.asSystemError
import okhttp3.OkHttpClient
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*
import java.util.logging.Level
import java.util.logging.Logger
import elements.Error as ElementsError

private val forTenSeconds = Wait.For(10, SECONDS)

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

            pusherino.assumeSuccess().subscribeToRoom(room, object : RoomSubscriptionListeners {
                override fun onNewMessage(message: Message) { messageReceived = message}
                override fun onError(error: elements.Error) { kotlin.error("error: $error") }
            })

            val messageResult = alice.assumeSuccess().sendMessage(room, "message text").wait(forTenSeconds)

            check(messageResult is Result.Success)
            assertThat(messageReceived.text).isEqualTo("message text")
            chat.close()
            aliceChat.close()
        }

    }

})

private fun <A> Result<A, elements.Error>.assumeSuccess(): A = when (this) {
    is Result.Success -> value
    is Result.Failure -> error("Failure: $error")
}

private val CurrentUser.generalRoom
    get() = rooms.find { it.name == "general" } ?: error("Could not find room general")

private fun chatFor(userName: String) = ChatManager(
    instanceLocator = INSTANCE_LOCATOR,
    userId = userName,
    dependencies = TestChatkitDependencies(
        tokenProvider = TestTokenProvider(INSTANCE_ID, userName, AUTH_KEY_ID, AUTH_KEY_SECRET)
    )
)
