package com.pusher.chatkit

import com.google.common.truth.Truth.*
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
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
            var userId by FutureValue<String?>()

            val sub = chat.connect { event ->
                userId = event.let { it as? CurrentUserReceived }?.currentUser?.id
            }

            assertThat(userId).isEqualTo(PUSHERINO)
            sub.unsubscribe()
        }

    }

})

private fun chatFor(userName: String) = ChatManager(
    instanceLocator = INSTANCE_LOCATOR,
    userId = userName,
    dependencies = TestChatkitDependencies(
        tokenProvider = TestTokenProvider(INSTANCE_ID, userName, AUTH_KEY_ID, AUTH_KEY_SECRET)
    )
)
