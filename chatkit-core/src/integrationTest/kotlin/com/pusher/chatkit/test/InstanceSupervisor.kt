package com.pusher.chatkit.test

import com.google.gson.JsonElement
import com.pusher.chatkit.*
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.tearDown
import com.pusher.chatkit.users.User
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.Futures
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import com.pusher.util.Result
import elements.Error
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * In charge of setting the right state of an intance for a test
 */
object InstanceSupervisor {

    /**
     * Calls set up without actions
     */
    fun tearDownInstance() = tearDown().run()

    /**
     * Tear downs the instance and runs the provided actions.
     */
    fun setUpInstanceWith(vararg actions: InstanceAction) {
        waitForIdleInstance().wait(Wait.For(60, TimeUnit.SECONDS))
        tearDown().run()
        newUser(SUPER_USER).run()
        actions.forEach { it.run() }
    }

}

private fun waitForIdleInstance(): Future<Result<Boolean, Error>> = Futures.schedule {
    generateSequence { isInstanceIdle() }
        .map {
            it.also {
                when {
                    it is Result.Success && !it.value -> Thread.sleep(1_000)
                    it is Result.Failure -> error(it.error)
                }
            }
        }
        .filter { it is Result.Success && it.value }
        .first()
}

private fun isInstanceIdle(): Result<Boolean, Error> = chatkitInstance.request(
    options = RequestOptions("/users", "GET"),
    tokenProvider = sudoTokenProvider,
    responseParser = { it.parseAs<List<User>>() }
).wait().map { it.isEmpty() }

private val sudoTokenProvider = TestTokenProvider(INSTANCE_ID, SUPER_USER, AUTH_KEY_ID, AUTH_KEY_SECRET, true)

private val chatkitInstance = Instance(
    locator = INSTANCE_LOCATOR,
    serviceName = "chatkit",
    serviceVersion = "v1",
    dependencies = TestDependencies()
)

typealias InstanceAction = () -> Future<Result<JsonElement, Error>>

fun InstanceAction.run() = this().wait().let { result ->
    check(result is Result.Success) { "Expected '$name' to success. Result was: $result" }
}

class NamedInstanceAction(val name: String, instanceAction: InstanceAction) : InstanceAction by instanceAction

fun InstanceAction.withName(name: String) = NamedInstanceAction(name, this)

val InstanceAction.name: String
    get() = when (this) {
        is NamedInstanceAction -> name
        else -> "No name"
    }

object InstanceActions {

    fun newUser(name: String): InstanceAction = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/users",
                method = "POST",
                body = name.toUserRequestBody()
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Create new user: $name")

    fun changeRoomName(room: Room, newName: String): InstanceAction = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/rooms/${room.id}",
                method = "PUT",
                body = """
                    {
                      "name" : "$newName"
                    }
                """.trimIndent()
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Changing name of room ${room.name} to $newName ")

    fun deleteRoom(room: Room): InstanceAction = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/rooms/${room.id}",
                method = "DELETE"
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Deleting room ${room.name} ")

    fun newUsers(vararg names: String): InstanceAction = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/batch_users",
                method = "POST",
                body = mapOf("users" to names.toList().toJsonString { it.toUserRequestBody() }).toJsonObject()
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Create new users: ${names.joinToString(", ")}")

    fun newRoom(name: String, vararg userNames: String, isPrivate: Boolean = false) = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/rooms",
                method = "POST",
                body = """
                    {
                        "name" : "$name",
                        "user_ids" : ${userNames.toList().toJsonString { "\"$it\"" }},
                        "private" : $isPrivate
                    }
                """.trimIndent()
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Create new room: $name for users: ${userNames.joinToString(", ")}")

    fun tearDown(): InstanceAction = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/resources",
                method = "DELETE"
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
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
