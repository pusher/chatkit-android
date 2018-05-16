package com.pusher.chatkit

import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.UserSubscription
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
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
internal const val SERVICE_VERSION = "v1"

class ChatManager constructor(
    private val instanceLocator: String,
    private val userId: String,
    internal val dependencies: ChatkitDependencies
) {

    val tokenProvider: TokenProvider = DebounceTokenProvider(
        dependencies.tokenProvider.also { (it as? ChatkitTokenProvider)?.userId = userId }
    )

    private val subscriptions = mutableListOf<Subscription>()
    private val eventConsumers = mutableListOf<ChatManagerEventConsumer>()
    private val apiInstance by lazyInstance(API_SERVICE_NAME, SERVICE_VERSION)

    internal val cursorService by lazy { CursorService(this) }
    internal val presenceService by lazy { PresenceService(this) }
    internal val userService by lazy { UserService(this) }
    internal val messageService by lazy { MessageService(this) }
    internal val filesService by lazy { FilesService(this) }
    internal val roomService by lazy { RoomService(this) }

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}): Future<Result<CurrentUser, Error>> {
        val futureCurrentUser = CurrentUserConsumer()
        eventConsumers += futureCurrentUser
        eventConsumers += consumer
        subscriptions += openSubscription()
        return futureCurrentUser.get()
    }

    private fun openSubscription() = UserSubscription(
        userId = userId,
        chatManager = this,
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

    internal fun lazyInstance(serviceName: String, serviceVersion: String) = lazy {
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
            responseParser = responseParser
        )

    internal fun <A> subscribeResuming(
        path: String,
        listeners: SubscriptionListeners<A>,
        messageParser: DataParser<A>
    ) = apiInstance.subscribeResuming(
        path = path,
        tokenProvider = tokenProvider,
        listeners = listeners,
        messageParser = messageParser
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

    fun Future<Result<Room, Error>>.saveRoomWhenReady() = mapResult {
        it.also { room ->
            chatManager.roomService.roomStore += room
            populateRoomUserStore(room)
        }
    }

    fun Future<Result<Int, Error>>.removeRoomWhenReady() = mapResult {
        it.also { roomId ->
            chatManager.roomService.roomStore -= roomId
        }
    }

    private fun populateRoomUserStore(room: Room) {
        chatManager.userService.populateUserStore(room.memberUserIds)
    }

}

