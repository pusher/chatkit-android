package com.pusher.chatkit.test

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.pusher.chatkit.AUTH_KEY_ID
import com.pusher.chatkit.AUTH_KEY_SECRET
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.INSTANCE_ID
import com.pusher.chatkit.INSTANCE_LOCATOR
import com.pusher.chatkit.TestDependencies
import com.pusher.chatkit.TestTokenProvider
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.createSuperUser
import com.pusher.chatkit.test.InstanceActions.setInstanceBusy
import com.pusher.chatkit.test.InstanceActions.tearDown
import com.pusher.chatkit.users.api.UserApiType
import com.pusher.chatkit.util.DateApiTypeMapper
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.Futures
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import com.pusher.util.Result
import elements.Error
import java.net.URLEncoder
import java.util.Date
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.junit.runner.notification.Failure

/**
 * In charge of setting the right state of an instance for a test
 */
object InstanceSupervisor {

    /**
     * Calls set up without actions
     */
    fun tearDownInstance() = tearDown().run()

    fun createRoles() = createDefaultRole().run()

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

private fun isInstanceIdle():
        Result<Boolean, Error> = chatkitInstance.request(
        options = RequestOptions("/users", "GET"),
        tokenProvider = sudoTokenProvider,
        responseParser = { it.parseAs<List<UserApiType>>() }
).wait().map { users ->
    users.firstOrNull { it.name == "lock" }?.takeUnless { it.wasCreatedLongerThan(5_000) } == null
}

private fun UserApiType.wasCreatedLongerThan(millisecondsAgo: Long): Boolean {
    return Date().time - DateApiTypeMapper().mapToEpochTime(createdAt) > millisecondsAgo
}

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
            serviceVersion = "v7",
            dependencies = TestDependencies()
    )
}

private val chatkitInstanceV2 by lazy {
    Instance(
            locator = INSTANCE_LOCATOR,
            serviceName = "chatkit",
            serviceVersion = "v2",
            dependencies = TestDependencies()
    )
}

private val authorizerInstance by lazy {
    Instance(
            locator = INSTANCE_LOCATOR,
            serviceName = "chatkit_authorizer",
            serviceVersion = "v2",
            dependencies = TestDependencies()
    )
}

private val cursorsInstance by lazy {
    Instance(
            locator = INSTANCE_LOCATOR,
            serviceName = "chatkit_cursors",
            serviceVersion = "v2",
            dependencies = TestDependencies()
    )
}

typealias InstanceAction = () -> Future<Result<JsonElement, Error>>

interface CompoundAction : InstanceAction

private fun compose(vararg actions: InstanceAction): CompoundAction = object : CompoundAction {
    override fun invoke(): Future<Result<JsonElement, Error>> {
        actions
                .map { it.name to it.invoke().wait() }
                .firstOrNull { (_, result) -> result is Failure }
                ?.let { (name, result) ->
                    error("Expected '$name' to success. Result was: $result")
                }
        return Futures.now(Result.success(JsonNull.INSTANCE as JsonElement))
    }
}

fun InstanceAction.run(): Unit =
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
            compose(newUser(SUPER_USER), createAdminRole(), setUserRole(SUPER_USER, "admin"))

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

    private fun createAdminRole(): InstanceAction = {
        authorizerInstance.request<JsonElement>(
                options = RequestOptions(
                        path = "/roles",
                        method = "POST",
                        body = mapOf(
                                "name" to "admin",
                                "permissions" to arrayOf(
                                        "message:create",
                                        "room:join",
                                        "room:leave",
                                        "room:members:add",
                                        "room:members:remove",
                                        "room:get",
                                        "room:create",
                                        "room:messages:get",
                                        "room:typing_indicator:create",
                                        "presence:subscribe",
                                        "user:get",
                                        "user:rooms:get",
                                        "cursors:read:get",
                                        "cursors:read:set",
                                        "file:create",
                                        "file:get",
                                        "room:delete",
                                        "room:update"
                                ),
                                "scope" to "global"
                        ).toJson()
                ),
                tokenProvider = sudoTokenProvider,
                responseParser = { it.parseAs() }
        )
    }.withName("Create admin role")

    fun createDefaultRole(): InstanceAction = {
        authorizerInstance.request<JsonElement>(
                options = RequestOptions(
                        path = "/roles",
                        method = "POST",
                        body = mapOf(
                                "name" to "default",
                                "permissions" to arrayOf(
                                        "message:create",
                                        "room:join",
                                        "room:leave",
                                        "room:members:add",
                                        "room:members:remove",
                                        "room:get",
                                        "room:create",
                                        "room:messages:get",
                                        "room:typing_indicator:create",
                                        "presence:subscribe",
                                        "user:get",
                                        "user:rooms:get",
                                        "cursors:read:get",
                                        "cursors:read:set",
                                        "file:create",
                                        "file:get"
                                ),
                                "scope" to "global"
                        ).toJson()
                ),
                tokenProvider = sudoTokenProvider,
                responseParser = { it.parseAs() }
        )
    }.withName("Create default role")

    fun newUser(
        id: String,
        name: String = "No name",
        avatarUrl: String = "https://gravatar.com/img/2124",
        customData: CustomData? = null
    ): InstanceAction = {
        chatkitInstance.request<JsonElement>(
                options = RequestOptions(
                        path = "/users",
                        method = "POST",
                        body = mapOf(
                                "name" to name,
                                "id" to id,
                                "avatar_url" to avatarUrl,
                                "custom_data" to customData
                        ).toJson()
                ),
                tokenProvider = sudoTokenProvider,
                responseParser = { it.parseAs() }
        )
    }.withName("Create new user: $id")

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

    fun newRoom(
        name: String,
        vararg userNames: String,
        pushNotificationTitleOverride: String? = null,
        isPrivate: Boolean = false,
        customData: CustomData? = null
    ) = {
        chatkitInstance.request<JsonElement>(
                options = RequestOptions(
                        path = "/rooms",
                        method = "POST",
                        body = mutableMapOf<String, Any?>(
                                "id" to name,
                                "name" to name,
                                "push_notification_title_override" to pushNotificationTitleOverride,
                                "user_ids" to userNames,
                                "private" to isPrivate
                        ).apply {
                            if (customData != null) this += "custom_data" to customData
                        }.toJson()
                ),
                tokenProvider = sudoTokenProvider,
                responseParser = { it.parseAs() }
        )
    }.withName("Create new room: $name for users: ${userNames.joinToString(", ")}")

    fun setCursor(userId: String, roomId: String, position: Int) = {
        cursorsInstance.request<JsonElement>(
                options = RequestOptions(
                        path = "/cursors/0/rooms/${URLEncoder.encode(roomId, "UTF-8")}/users/${URLEncoder.encode(userId, "UTF-8")}",
                        method = "PUT",
                        body = mutableMapOf<String, Any?>("position" to position).toJson()
                ),
                tokenProvider = sudoTokenProvider,
                responseParser = { it.parseAs() }
        )
    }.withName("Set cursor")

    fun deleteMessage(roomId: String, messageId: Int) = {
        chatkitInstance.request<JsonElement>(
                options = RequestOptions(
                        path = "/rooms/${URLEncoder.encode(roomId, "UTF-8")}/messages/$messageId",
                        method = "DELETE"

                ),
                tokenProvider = sudoTokenProvider,
                responseParser = { it.parseAs() }
        )
    }.withName("Deleting message $messageId")

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
