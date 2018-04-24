package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.test.Action.*
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceSupervisor
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstance
import com.pusher.chatkit.test.will
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import elements.Error as ElementsError

class ChatManagerSpek : Spek({

    afterEachTest(InstanceSupervisor::tearDownInstance)

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
            setUpInstance(CreateUser(USER_NAME))

            var user by FutureValue<CurrentUser?>()
            val sub = manager.connect { event ->
                    when(event) {
                        is ErrorOccurred -> fail(event.error.reason)
                        is CurrentUserReceived ->  user = event.currentUser
                        else -> println(event)
                    }
            }

            done {
                assertThat(user?.id).isEqualTo(USER_NAME)
                sub.unsubscribe()
            }
        }

        will("load user rooms", TIMEOUT) {
            setUpInstance(
                CreateUser(USER_NAME),
                CreateRoom("general", listOf(USER_NAME))
            )

            var rooms by FutureValue<List<Room>?>()
            val sub = manager.connect { event ->
                when(event) {
                    is ErrorOccurred -> fail(event.error.reason)
                    is CurrentUserReceived -> rooms = event.currentUser.rooms
                    else -> println(event)
                }
            }

            done {
                assertThat(rooms?.map { it.name }).containsExactly("general")
                sub.unsubscribe()
            }
        }

    }

})

