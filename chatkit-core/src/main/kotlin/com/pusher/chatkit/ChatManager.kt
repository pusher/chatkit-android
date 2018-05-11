package com.pusher.chatkit

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomStore
import com.pusher.chatkit.users.UserSubscription
import com.pusher.chatkit.users.*
import com.pusher.platform.*
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue

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

    internal val apiInstance by lazyInstance(API_SERVICE_NAME, SERVICE_VERSION)
    internal val cursorsInstance by lazyInstance(CURSOR_SERVICE_NAME, SERVICE_VERSION)
    internal val filesInstance by lazyInstance(FILES_SERVICE_NAME, SERVICE_VERSION)
    internal val presenceInstance by lazyInstance(PRESENCE_SERVICE_NAME, SERVICE_VERSION)

    internal val roomStore by lazy { RoomStore() }

    private val subscriptions = mutableListOf<Subscription>()
    private val eventConsumers = mutableListOf<ChatManagerEventConsumer>()

    internal val cursorService by lazy { CursorService(this) }
    internal val presenceService by lazy { PresenceService(this) }
    internal val userService by lazy { UserService(this) }

    init {
        if (tokenProvider is ChatkitTokenProvider) {
            tokenProvider.userId = userId
        }
    }

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}): Future<Result<CurrentUser, Error>> =
        CurrentUserConsumer()
            .also { currentUserConsumer ->
                eventConsumers += currentUserConsumer
                eventConsumers += consumer
                subscriptions += openSubscription()
            }
            .get()

    private fun openSubscription() = UserSubscription(
        userId = userId,
        chatManager = this@ChatManager,
        consumeEvent = { event -> eventConsumers.forEach { it(event) } }
    )

    private class CurrentUserConsumer: ChatManagerEventConsumer {

        val queue = SynchronousQueue<Result<CurrentUser, Error>>()
        var waitingForUser = true

        override fun invoke(event: ChatManagerEvent) {
            if (waitingForUser) {
                when (event) {
                    is ChatManagerEvent.CurrentUserReceived -> queue.put(event.currentUser.asSuccess())
                    is ChatManagerEvent.ErrorOccurred -> queue.put(event.error.asFailure())
                }
            }
        }

        fun get() = Futures.schedule {
            queue.take().also { waitingForUser = false }
        }

    }

    fun connect(listeners: ChatManagerListeners): Future<Result<CurrentUser, Error>> =
        connect(listeners.toCallback())

    internal fun observerEvents(consumer: ChatManagerEventConsumer) {
        eventConsumers += consumer
    }

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

    internal inline fun <reified A> doPost(
        path: String,
        body: String = "",
        noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
        doRequest("POST", path, body, responseParser)

    internal inline fun <reified A> doPut(
        path: String,
        body: String = "",
        noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
        doRequest("PUT", path, body, responseParser)

    internal inline fun <reified A> doGet(
        path: String,
        noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
        doRequest("GET", path, null, responseParser)

    internal inline fun <reified A> doDelete(
        path: String,
        noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
        doRequest("DELETE", path, null, responseParser)

    private fun <A> doRequest(
        method: String,
        path: String,
        body: String?,
        responseParser: DataParser<A>
    ): Future<Result<A, Error>> =
        apiInstance.request(
            options = RequestOptions(
                method = method,
                path = path,
                body = body
            ),
            tokenProvider = tokenProvider,
            tokenParams = dependencies.tokenParams,
            responseParser = responseParser
        )

    /**
     * Tries to close all pending subscriptions and resources
     */
    fun close(): Result<Unit, Error> = try {
        subscriptions.forEach { it.unsubscribe() }
        dependencies.okHttpClient?.connectionPool()?.evictAll()
        eventConsumers.clear()
        Unit.asSuccess()
    } catch (e: Throwable) {
        Errors.other(e).asFailure()
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
        chatManager.userService.populateUserStore(room.memberUserIds)
    }

}

internal data class ChatEvent(
    val eventName: String,
    override val userId: String = "",
    val timestamp: String,
    val data: JsonElement
) : HasUser

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
