package com.pusher.chatkit

import com.google.common.truth.Truth
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.Timeout
import com.pusher.chatkit.test.will
import elements.Subscription
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import elements.Error as ElementsError

private val INSTANCE_LOCATOR: String = System.getProperty("chatkit_integration_locator") ?: "Missing gradle/system property 'chatkit_integration_locator'"
private val INSTANCE_ID = INSTANCE_LOCATOR.split(":").getOrNull(2) ?: "Missing instance id in locator (property 'chatkit_integration_locator')"
private val USER_NAME: String = System.getProperty("chatkit_integration_username") ?: "Missing gradle/system property 'chatkit_integration_username'"

class ChatManagerSpek : Spek({

    describe("ChatManager with valid instance") {

        val manager = ChatManager(
            instanceLocator = INSTANCE_LOCATOR,
            userId = USER_NAME,
            dependencies = TestChatkitDependencies(
                tokenProvider = TestTokenProvider(INSTANCE_ID, USER_NAME)
            )
        )

        will("load current user") {
            var sub by FutureValue<Subscription>()
            sub = manager.connect { event ->
                done {
                    val currentUser = event.let { it as? CurrentUserReceived }?.currentUser
                    Truth.assertThat(currentUser?.id).isEqualTo(USER_NAME)
                    sub.unsubscribe()
                }
            }
        }

    }

})

