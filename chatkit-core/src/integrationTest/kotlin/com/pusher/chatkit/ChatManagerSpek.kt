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
import com.pusher.chatkit.test.ResultAssertions.assertSuccess
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import elements.Error as ElementsError

class ChatManagerSpek : Spek({

    afterEachTest(::tearDownInstance)

    describe("ChatManager with valid instance") {

        it("loads current user") {
            setUpInstanceWith(newUser(PUSHERINO))
            val chat = chatFor(PUSHERINO)
            var userId by FutureValue<String>()

            chat.connect(onCurrentUserReceived { currentUser ->
                userId = currentUser.id
            })

            assertThat(userId).isEqualTo(PUSHERINO)
            chat.close()
        }

        it("loads user rooms") {
            setUpInstanceWith(newUser(PUSHERINO), newRoom("general", PUSHERINO))
            val chat = chatFor(PUSHERINO)
            var roomNames by FutureValue<List<String>>()

            chat.connect(onCurrentUserReceived { currentUser ->
                roomNames = currentUser.rooms.map { it.name }
            })

            assertThat(roomNames).containsExactly("general")
            chat.close()
        }

        it("loads users related to current user") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom("general", PUSHERINO, ALICE))
            val chat = chatFor(PUSHERINO)
            var relatedUserIds by FutureValue<List<String>>()

            chat.connect(onCurrentUserReceived { currentUser ->
                currentUser.users.onReady {
                    relatedUserIds =it.recover { emptyList() }.map { it.id }
                }
            })

            assertThat(relatedUserIds).containsAllOf("alice", PUSHERINO)
            chat.close()
        }

        it("subscribes to a room and receives message from alice") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom("general", PUSHERINO, ALICE))
            val chat = chatFor(PUSHERINO)
            val aliceChat = chatFor(ALICE)

            var messageReceived by FutureValue<Message>()

            chat.connect(onCurrentUserReceived { pusherino ->
                pusherino.subscribeToRoom(pusherino.generalRoom, object : RoomSubscriptionListeners {
                    override fun onNewMessage(message: Message) { messageReceived = message }
                    override fun onError(error: elements.Error) = error("room subscription error: $error")
                })
            })

            aliceChat.connect(onCurrentUserReceived { alice ->
                alice.sendMessage(alice.generalRoom, "message text")
                    .onReady { assertSuccess(it) }
            })

            assertThat(messageReceived.text).isEqualTo("message text")
            chat.close()
            aliceChat.close()
        }

    }

})

private val CurrentUser.generalRoom
    get() = rooms.find { it.name == "general" } ?: error("Could not find room general")

private fun chatFor(userName: String) = ChatManager(
    instanceLocator = INSTANCE_LOCATOR,
    userId = userName,
    dependencies = TestChatkitDependencies(
        tokenProvider = TestTokenProvider(INSTANCE_ID, userName, AUTH_KEY_ID, AUTH_KEY_SECRET)
    )
)

private fun onCurrentUserReceived(
    block: (CurrentUser) -> Unit
): (ChatManagerEvent) -> Unit = { event: ChatManagerEvent ->
    when (event) {
        is ErrorOccurred -> error(event.error.reason)
        is CurrentUserReceived -> block(event.currentUser)
        else -> println(event)
    }
}
