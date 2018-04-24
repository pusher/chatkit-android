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
    fun tearDownInstance() = setUpInstanceWith()

    /**
     * Tear downs the instance and runs the provided actions.
     */
    fun setUpInstanceWith(vararg actions: InstanceAction) = (listOf(InstanceActions.tearDown()) + actions)
        .map { it.blockfor() }
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

typealias InstanceAction = () -> OkHttpResponsePromise

private fun InstanceAction.blockfor(): OkHttpResponseResult {
    var result by FutureValue<OkHttpResponseResult>()
    this().onReady { result = it }
    return result
}

object InstanceActions {

    fun newUser(name: String): InstanceAction {

        return {
            chatkitInstance.request(
                options = RequestOptions(
                    path = "/users",
                    method = "POST",
                    body = name.toUserRequestBody()
                ),
                tokenProvider = sudoTokenProvider
            )
        }
    }

    fun newUsers(vararg names: String): InstanceAction = {
        chatkitInstance.request(
            options = RequestOptions(
                path = "/batch_users",
                method = "POST",
                body = mapOf("users" to names.toList().toJsonString { it.toUserRequestBody() }).toJsonObject()
            ),
            tokenProvider = sudoTokenProvider
        )
    }

    fun newRoom(name: String, vararg userNames: String) = {
        chatkitInstance.request(
            options = RequestOptions(
                path = "/rooms",
                method = "POST",
                body = """
                    {
                        "name": "$name",
                        "user_ids": ${userNames.toList().toJsonString { "\"$it\"" } }
                    }
                """.trimIndent()
            ),
            tokenProvider = sudoTokenProvider
        )
    }

    fun tearDown(): InstanceAction = {
        chatkitInstance.request(
            options = RequestOptions(
                path = "/resources",
                method = "DELETE"
            ),
            tokenProvider = sudoTokenProvider
        )
    }

}

fun String.toUserRequestBody() =
    """
        {
          "name": "No name",
          "id": "$this",
          "avatar_url": "https://gravatar.com/img/2124"
        }
    """

private fun Iterable<String>.toJsonString(block: (String) -> CharSequence) =
    joinToString(", ","[","]", transform = block)

private fun Map<String, Any>.toJsonObject() =
    entries.joinToString(", ", "{", "}") { (key, value) -> "\"$key\" : $value" }
