package com.pusher.chatkit.test

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.pusher.chatkit.*
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.test.InstanceActions.createSuperUser
import com.pusher.chatkit.test.InstanceActions.setInstanceBusy
import com.pusher.chatkit.test.InstanceActions.tearDown
import com.pusher.chatkit.users.User
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.Futures
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import com.pusher.util.Result
import com.pusher.util.flatMapFutureResult
import elements.Error
import org.junit.runner.notification.Failure
import java.util.*
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
        listOf(tearDown(), setInstanceBusy(), createSuperUser())
            .plus(actions)
            .forEach(InstanceAction::run)
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
).wait().map { users ->
    users.firstOrNull { it.name == "lock" }?.takeUnless { it.wasCreatedLongerThan(5_000) } == null
}

private fun User.wasCreatedLongerThan(millisecondsAgo: Long) =
    Date().time - created.time > millisecondsAgo

private val sudoTokenProvider by lazy {
    TestTokenProvider(
        instanceId = checkNotNull(INSTANCE_ID) { "Missing 'chatkit_integration_locator' property" },
        userId = SUPER_USER,
        keyId = AUTH_KEY_ID,
        secret = AUTH_KEY_SECRET,
        su = true
    )
}

private val chatkitInstance by lazy {
    Instance(
        locator = INSTANCE_LOCATOR,
        serviceName = "chatkit",
        serviceVersion = "v1",
        dependencies = TestDependencies()
    )
}

private val authorizerInstance by lazy {
    Instance(
        locator = INSTANCE_LOCATOR,
        serviceName = "chatkit_authorizer",
        serviceVersion = "v1",
        dependencies = TestDependencies()
    )
}

typealias InstanceAction = () -> Future<Result<JsonElement, Error>>

interface CompoundAction : InstanceAction

private fun compose(vararg actions: InstanceAction): CompoundAction = object : CompoundAction {
    override fun invoke(): Future<Result<JsonElement, Error>> {
        actions
            .map { it.name to it.invoke().wait() }
            .firstOrNull { (name, result) -> result is Failure }
            ?.let { (name, result) ->
                error("Expected '$name' to success. Result was: $result")
            }
        return Futures.now(Result.success(JsonNull.INSTANCE as JsonElement))
    }
}

fun InstanceAction.run() : Unit =
    invoke().wait().let { result ->
        if (this !is CompoundAction) check(result is Result.Success) { "Expected '$name' to success. Result was: $result" }
    }

class NamedInstanceAction(val name: String, instanceAction: InstanceAction) : InstanceAction by instanceAction

fun InstanceAction.withName(name: String) = NamedInstanceAction(name, this)

val InstanceAction.name: String
    get() = when (this) {
        is NamedInstanceAction -> name
        else -> "No name"
    }

object InstanceActions {

    fun setInstanceBusy(): InstanceAction =
        newUser(id = "lock")::invoke.withName("Set instance Busy")

    fun createSuperUser(): InstanceAction =
        compose(newUser(SUPER_USER), createAdminRole(), setUserRole(SUPER_USER, "admin") )

    fun setUserRole(userId: String, role: String): InstanceAction = {
        authorizerInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/users/$userId/roles",
                method = "PUT",
                body = mapOf("name" to role).toJson()
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Assign role $role to $userId")

    fun createAdminRole(): InstanceAction = {
        authorizerInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/roles",
                method = "POST",
                body = arrayOf(
                    mapOf(
                        "name" to "admin",
                        "permissions" to arrayOf("room:create", "user:update"),
                        "scope" to "global"
                    )
                ).toJson()
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Create admin role")

    fun newUser(
        id: String,
        name: String = "No name",
        avatarUrl: String = "https://gravatar.com/img/2124"
    ): InstanceAction = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/users",
                method = "POST",
                body = mapOf(
                    "name" to name,
                    "id" to id,
                    "avatar_url" to avatarUrl
                ).toJson()
            ),
            tokenProvider = sudoTokenProvider,
            responseParser = { it.parseAs() }
        )
    }.withName("Create new user: $id")

    fun changeRoomName(room: Room, newName: String): InstanceAction = {
        chatkitInstance.request<JsonElement>(
            options = RequestOptions(
                path = "/rooms/${room.id}",
                method = "PUT",
                body = mapOf("name" to newName).toJson()
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
                body = mapOf("users" to names.toList().map {
                    mapOf(
                        "name" to "No name",
                        "id" to it,
                        "avatar_url" to "https://gravatar.com/img/2124"
                    )
                }).toJson()
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
                body = mapOf(
                    "name" to name,
                    "user_ids" to userNames,
                    "private" to isPrivate
                ).toJson()
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

private fun <A> A.toJson(): String {

    fun <A> Iterable<A>.toJsonArray(): String =
        joinToString(", ", "[", "]") { it.toJson() }

    fun <A, B> Map<A, B>.toJsonObject() =
        entries.joinToString(", ", "{", "}") { (key, value) -> "\"$key\" : ${value.toJson()}" }

    return when (this) {
        null -> "null"
        is String -> "\"$this\""
        is Array<*> -> this.toList().toJsonArray()
        is Iterable<*> -> this.toJsonArray()
        is Map<*, *> -> this.toJsonObject()
        else -> toString()
    }

}
