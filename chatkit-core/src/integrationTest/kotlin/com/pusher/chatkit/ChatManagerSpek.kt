package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.test.Action
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceSupervisor
import com.pusher.chatkit.test.will
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import elements.Error as ElementsError

class ChatManagerSpek : Spek({

    afterEachTest(InstanceSupervisor::tearDown)

    describe("ChatManager with valid instance") {

        InstanceSupervisor.setUp(Action.CreateUser("pusherino"))

        val manager = ChatManager(
            instanceLocator = INSTANCE_LOCATOR,
            userId = USER_NAME,
            dependencies = TestChatkitDependencies(
                tokenProvider = TestTokenProvider(INSTANCE_ID, USER_NAME, AUTH_KEY_ID, AUTH_KEY_SECRET)
            )
        )

        will("load current user", TIMEOUT) {
            var user by FutureValue<CurrentUser?>()
            val sub = manager.connect { event ->
                user = event.let { it as? CurrentUserReceived }?.currentUser
            }

            done {
                assertThat(user?.id).isEqualTo(USER_NAME)
                sub.unsubscribe()
            }
        }

    }

})

