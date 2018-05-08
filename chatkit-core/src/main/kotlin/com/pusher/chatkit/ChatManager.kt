package com.pusher.chatkit

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.typeToken
import com.pusher.chatkit.rooms.HasRoom
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.users.HasUser
import com.pusher.chatkit.users.UserService
import com.pusher.platform.*
import com.pusher.platform.network.Futures
import com.pusher.platform.network.wait
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Subscription
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue

private const val USERS_PATH = "users"
private const val API_SERVICE_NAME = "chatkit"
private const val CURSOR_SERVICE_NAME = "chatkit_cursors"
private const val SERVICE_VERSION = "v1"
private const val FILES_SERVICE_NAME = "chatkit_files"
private const val PRESENCE_SERVICE_NAME = "chatkit_presence"

class ChatManager constructor(
    private val instanceLocator: String,
    private val userId: String,
    internal val dependencies: ChatkitDependencies
) {

    val tokenProvider: TokenProvider = DebounceTokenProvider(dependencies.tokenProvider)

    companion object {
        val GSON: Gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }

    private val logger = dependencies.logger

    internal val apiInstance by lazyInstance(API_SERVICE_NAME, SERVICE_VERSION)
    internal val cursorsInstance by lazyInstance(CURSOR_SERVICE_NAME, SERVICE_VERSION)
    internal val filesInstance by lazyInstance(FILES_SERVICE_NAME, SERVICE_VERSION)
    internal val presenceInstance by lazyInstance(PRESENCE_SERVICE_NAME, SERVICE_VERSION)

    internal val userStore by lazy { GlobalUserStore() }
    internal val roomStore by lazy { RoomStore() }

    private val subscriptions = mutableListOf<Subscription>()

    init {
        if (tokenProvider is ChatkitTokenProvider) {
            tokenProvider.userId = userId
        }
    }

    fun connect(consumer: ChatManagerEventConsumer = {}): Future<Result<CurrentUser, Error>> = Futures.schedule {
        val queue = SynchronousQueue<Result<CurrentUser, Error>>()
        var waitingForUser = true
        subscriptions += UserSubscription(
            userId = userId,
            chatManager = this@ChatManager,
            path = USERS_PATH,
            userStore = userStore,
            tokenProvider = tokenProvider,
            tokenParams = dependencies.tokenParams,
            logger = logger,
            consumeEvent = { event ->
                consumer(event)
                if (waitingForUser) {
                    when (event) {
                        is ChatManagerEvent.CurrentUserReceived -> queue.put(event.currentUser.asSuccess())
                        is ChatManagerEvent.ErrorOccurred -> queue.put(event.error.asFailure())
                    }
                }
            }
        )
        queue.take().also {
            waitingForUser = false
        }
    }

    fun connect(listeners: ChatManagerListeners): Future<Result<CurrentUser, Error>> =
        connect(listeners.toCallback())

    internal fun roomService(): RoomService =
        RoomService(this)

    internal fun userService(): UserService =
        UserService(this)

    private fun lazyInstance(serviceName: String, serviceVersion: String) = lazy {
        val instance = Instance(
            locator = instanceLocator,
            serviceName = serviceName,
            serviceVersion = serviceVersion,
            dependencies = dependencies
        )
        dependencies.okHttpClient?.let { client ->
            instance.copy(baseClient = instance.baseClient.copy(client = client))
        } ?: instance
    }

    internal inline fun <reified A> doPost(path: String, body: String = ""): Future<Result<A, Error>> =
        doRequest("POST", path, body)

    internal inline fun <reified A> doPut(path: String, body: String = ""): Future<Result<A, Error>> =
        doRequest("PUT", path, body)

    internal inline fun <reified A> doGet(path: String): Future<Result<A, Error>> =
        doRequest("GET", path, null)

    internal inline fun <reified A> doDelete(path: String): Future<Result<A, Error>> =
        doRequest("DELETE", path, null)

    private inline fun <reified A> doRequest(method: String, path: String, body: String?): Future<Result<A, Error>> =
        apiInstance.request(
            options = RequestOptions(
                method = method,
                path = path,
                body = body
            ),
            tokenProvider = tokenProvider,
            tokenParams = dependencies.tokenParams,
            responseParser = { it.parseAs<A>() }
        )

    /**
     * Tries to close all pending subscriptions and resources
     */
    fun close() {
        subscriptions.forEach { it.unsubscribe() }
        dependencies.okHttpClient?.connectionPool()?.evictAll()
    }



}

internal interface HasChat {

    val chatManager: ChatManager

    fun Future<Result<Room, Error>>.updateStoreWhenReady() = mapResult {
        it.also { room ->
            chatManager.roomStore += room
            populateRoomUserStore(room)
        }
    }

    fun populateRoomUserStore(room: Room) {
        chatManager.userService().populateUserStore(room.memberUserIds)
    }

}

data class Message(
    val id: Int,
    override val userId: String,
    override val roomId: Int,
    val text: String? = null,
    val attachment: Attachment? = null,
    val createdAt: String,
    val updatedAt: String
) : HasRoom, HasUser

data class Attachment(
    @Transient var fetchRequired: Boolean = false,
    @SerializedName("resource_link") val link: String,
    val type: String
)

data class Cursor(
    override val userId: String,
    override val roomId: Int,
    val type: Int,
    val position: Int,
    val updatedAt: String
) : HasUser, HasRoom

data class ChatEvent(
    val eventName: String,
    override val userId: String = "",
    val timestamp: String,
    val data: JsonElement
) : HasUser

typealias CustomData = MutableMap<String, String>

/**
 * Used to avoid multiple requests to the tokenProvider if one is pending
 */
private class DebounceTokenProvider(
    val original: TokenProvider
) : TokenProvider {

    private var pending: Future<Result<String, Error>>? = null

    override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> = synchronized(this) {
        pending ?: original.fetchToken(tokenParams).also { pending = it }
    }

    override fun clearToken(token: String?) = synchronized(this) {
        original.clearToken(token)
        pending = null
    }

}
