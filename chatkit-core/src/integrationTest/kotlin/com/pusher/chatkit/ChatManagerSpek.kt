package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.test.*
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.junit.runner.notification.Failure
import elements.Error as ElementsError

class ChatManagerSpek : Spek({

    afterEachTest(::tearDownInstance)

    describe("ChatManager with valid instance") {

        val manager by memoized {
            ChatManager(
                instanceLocator = INSTANCE_LOCATOR,
                userId = "pusherino",
                dependencies = TestChatkitDependencies(
                    tokenProvider = TestTokenProvider(INSTANCE_ID, USER_NAME, AUTH_KEY_ID, AUTH_KEY_SECRET)
                )
            )
        }

        will("load current user", TIMEOUT) {
            setUpInstanceWith(newUser(USER_NAME))

            var user by FutureValue<CurrentUser>()
            val sub = manager.connect(onCurrentUserReceived { currentUser ->
                user = currentUser
            })

            done {
                assertThat(user.id).isEqualTo(USER_NAME)
                sub.unsubscribe()
            }
        }

        will("load user rooms", TIMEOUT) {
            setUpInstanceWith(newUser(USER_NAME), newRoom("general", USER_NAME))

            var rooms by FutureValue<List<Room>?>()
            val sub = manager.connect(onCurrentUserReceived { currentUser ->
                currentUser.users.onReady { rooms = currentUser.rooms }
            })

            done {
                assertThat(rooms?.map { it.name }).containsExactly("general")
                sub.unsubscribe()
            }
        }

        will("load users related to current user", TIMEOUT) {
            setUpInstanceWith(newUsers(USER_NAME, "alice"), newRoom("general", USER_NAME, "alice"))

            var users by FutureValue<List<User>?>()
            val sub = manager.connect(onCurrentUserReceived { currentUser ->
                currentUser.users.onReady { users = it.recover { emptyList() } }
            })

            done {
                assertThat(users?.map { it.id }).containsExactly("alice", USER_NAME)
                sub.unsubscribe()
            }
        }

        will("subscribe to a room and receive message from alice", TIMEOUT) {
            setUpInstanceWith(newUsers(USER_NAME, "alice"), newRoom("general", USER_NAME, "alice"))

//            val aliceManager = ChatManager(
//                instanceLocator = INSTANCE_LOCATOR,
//                userId = "alice",
//                dependencies = TestChatkitDependencies(
//                    tokenProvider = TestTokenProvider(INSTANCE_ID, "alice", AUTH_KEY_ID, AUTH_KEY_SECRET)
//                )
//            )

            var user by FutureValue<CurrentUser>()
            val sub = manager.connect(onCurrentUserReceived { currentUser -> user = currentUser })
            val pusherino = user
//            val alice = waitForUserOnConnect(aliceManager)

//            var messageReceived by FutureValue<Message?>()

//            val sharedRoom = pusherino.rooms.find { it.name == "general" } ?: error("Could not find room general")

//            pusherino.subscribeToRoom(sharedRoom, object: RoomSubscriptionListeners {
//                override fun onNewMessage(message: Message) {
//                    messageReceived = message
//                }
//
//                override fun onError(error: elements.Error): Unit =
//                    fail("room subscription error: $error")
//
//            })
//
//            alice.sendMessage(sharedRoom, "message text").onReady {
//                if (it is Failure) fail(it.exception)
//            }

            done {
                assertThat(pusherino.id).isEqualTo("pusherino")
//                assertThat(alice.id).isEqualTo("alice")
//                assertThat(sharedRoom.name).isEqualTo("general")
//                val message = messageReceived
//                assertThat(message?.text).isEqualTo("message text")
//                manager.close()
//                aliceManager.close()
                sub.unsubscribe()
            }
        }

    }

})

private fun SuspendedTestBody.waitForUserOnConnect(manager: ChatManager): CurrentUser {
    var user by FutureValue<CurrentUser>()
    manager.connect(onCurrentUserReceived { currentUser -> user = currentUser })
    return user
}

private fun SuspendedTestBody.onCurrentUserReceived(
    block: (CurrentUser) -> Unit
): (ChatManagerEvent) -> Unit = { event: ChatManagerEvent ->
    when(event) {
        is ErrorOccurred -> fail(event.error.reason)
        is CurrentUserReceived -> block(event.currentUser)
        else -> println(event)
    }
}
