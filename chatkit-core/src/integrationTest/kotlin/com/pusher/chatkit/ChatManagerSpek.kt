package com.pusher.chatkit

import com.google.common.truth.Truth
import com.google.common.truth.Truth.*
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.Timeout
import com.pusher.chatkit.test.will
import elements.Subscription
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import elements.Error as ElementsError

private fun systemProperty(name: String) =
    System.getProperty(name) ?: "Missing gradle/system property '$name'"

/**
 * These should be added in ~/.gradle/gradle.properties or as a system property. If running them on the IDE, they must be a system property.
 */
private val INSTANCE_LOCATOR: String = systemProperty("chatkit_integration_locator")
private val INSTANCE_ID = INSTANCE_LOCATOR.split(":").getOrNull(2) ?: "Missing instance id in locator (property 'chatkit_integration_locator')"
private val USER_NAME: String = systemProperty("chatkit_integration_username")
private val AUTH_KEY: String = systemProperty("chatkit_integration_key")
private val AUTH_KEY_ID: String = AUTH_KEY.split(":")[0]
private val AUTH_KEY_SECRET: String = AUTH_KEY.split(":")[1]
private val TIMEOUT = (System.getProperty("chatkit_integration_timeout") ?: "-1").toLongOrNull().let { timeout ->
    when {
        timeout == null -> Timeout.Some(5000)
        timeout <= 0 -> Timeout.None
        else -> Timeout.Some(timeout)
    }
}

class ChatManagerSpek : Spek({

    describe("ChatManager with valid instance") {

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

