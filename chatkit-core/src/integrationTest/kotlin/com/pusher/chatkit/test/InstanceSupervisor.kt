package com.pusher.chatkit.test

import com.pusher.chatkit.*
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.tearDown
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.network.OkHttpResponseResult
import com.pusher.util.Result
import junit.framework.TestCase

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
    fun setUpInstanceWith(vararg actions: InstanceAction) =
        (listOf(tearDown(), newUser(SUPER_USER)) + actions)
            .map { it.name to it.blockFor() }
            .forEach { (name, result) ->
                when (result) {
                    is Result.Success -> result.value.let { response ->
                        check(response.isSuccessful) { "Expected '$name' to success. Request was: $response" }
                    }
                    is Result.Failure -> TestCase.fail("Expected '$name' to success. Result was: $result")
                }
            }

}


private val sudoTokenProvider = TestTokenProvider(INSTANCE_ID, SUPER_USER, AUTH_KEY_ID, AUTH_KEY_SECRET, true)

private val chatkitInstance = Instance(
    locator = INSTANCE_LOCATOR,
    serviceName = "chatkit",
    serviceVersion = "v1",
    dependencies = TestDependencies()
)

typealias InstanceAction = () -> OkHttpResponsePromise

class NamedInstanceAction(val name: String, instanceAction: InstanceAction) : InstanceAction by instanceAction

fun InstanceAction.withName(name: String) = NamedInstanceAction(name, this)

val InstanceAction.name: String
    get() = when (this) {
        is NamedInstanceAction -> name
        else -> "No name"
    }

private fun InstanceAction.blockFor(): OkHttpResponseResult {
    var result by FutureValue<OkHttpResponseResult>()
    this().onReady { result = it }
    return result
}

object InstanceActions {

    fun newUser(name: String): InstanceAction = {
        chatkitInstance.request(
            options = RequestOptions(
                path = "/users",
                method = "POST",
                body = name.toUserRequestBody()
            ),
            tokenProvider = sudoTokenProvider
        )
    }.withName("Create new user: $name")

    fun newUsers(vararg names: String): InstanceAction = {
        chatkitInstance.request(
            options = RequestOptions(
                path = "/batch_users",
                method = "POST",
                body = mapOf("users" to names.toList().toJsonString { it.toUserRequestBody() }).toJsonObject()
            ),
            tokenProvider = sudoTokenProvider
        )
    }.withName("Create new users: ${names.joinToString(", ")}")

    fun newRoom(name: String, vararg userNames: String) = {
        chatkitInstance.request(
            options = RequestOptions(
                path = "/rooms",
                method = "POST",
                body = """
                    {
                        "name": "$name",
                        "user_ids": ${userNames.toList().toJsonString { "\"$it\"" }}
                    }
                """.trimIndent()
            ),
            tokenProvider = sudoTokenProvider
        )
    }.withName("Create new room: $name for users: ${userNames.joinToString(", ")}")

    fun tearDown(): InstanceAction = {
        chatkitInstance.request(
            options = RequestOptions(
                path = "/resources",
                method = "DELETE"
            ),
            tokenProvider = sudoTokenProvider
        )
    }.withName("Tear down")

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
    joinToString(", ", "[", "]", transform = block)

private fun Map<String, Any>.toJsonObject() =
    entries.joinToString(", ", "{", "}") { (key, value) -> "\"$key\" : $value" }
