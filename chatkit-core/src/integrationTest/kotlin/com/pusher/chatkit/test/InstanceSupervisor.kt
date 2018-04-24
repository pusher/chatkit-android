package com.pusher.chatkit.test

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.*
import com.pusher.chatkit.test.ResultAssertions.onSuccess
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.network.OkHttpResponseResult
import okhttp3.Response

/**
 * In charge of setting the right state of an intance for a test
 */
object InstanceSupervisor {

    /**
     * Calls set up without actions
     */
    fun tearDownInstance() = setUpInstance()

    /**
     * Tear downs the instance and runs the provided actions.
     */
    fun setUpInstance(vararg actions: Action) = (listOf(Action.TearDown) + actions)
        .map { it.runAcync() }
        .forEach { onSuccess(it) { it.assertIsSuccessful() } }

}

private fun Response.assertIsSuccessful() =
    assertThat(isSuccessful).named("expected success request: $this").isTrue()


private val sudoTokenProvider = TestTokenProvider(INSTANCE_ID, USER_NAME, AUTH_KEY_ID, AUTH_KEY_SECRET, true)

private val chatkitInstance = Instance(
    locator = INSTANCE_LOCATOR,
    serviceName = "chatkit",
    serviceVersion = "v1",
    dependencies = TestDependencies()
)

sealed class Action(val run: () -> OkHttpResponsePromise) {

    data class CreateUser(val userName: String) : Action({
        chatkitInstance.request(
            options = RequestOptions(
                path = "/users",
                method = "POST",
                body = """
                    {
                      "name": "Pusher Ino",
                      "id": "$userName",
                      "avatar_url": "https://gravatar.com/img/2124"
                    }
                """.trimIndent()
            ),
            tokenProvider = sudoTokenProvider
        )

    })

    data class CreateRoom(val roomName: String, val userNames: List<String>) : Action({
        chatkitInstance.request(
            options = RequestOptions(
                path = "/rooms",
                method = "POST",
                body = """
                    {
                        "name": "$roomName",
                        "user_ids": ${userNames.toJsonString()}
                    }
                """.trimIndent()
            ),
            tokenProvider = sudoTokenProvider
        )
    })

    object TearDown : Action({
        chatkitInstance.request(
            options = RequestOptions(
                path = "/resources",
                method = "DELETE"
            ),
            tokenProvider = sudoTokenProvider
        )
    })

    fun runAcync(): OkHttpResponseResult {
        var result by FutureValue<OkHttpResponseResult>()
        run().onReady { result = it }
        return result
    }

}

private fun List<String>.toJsonString() =
    joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]"
    ) { "\"$it\"" }
