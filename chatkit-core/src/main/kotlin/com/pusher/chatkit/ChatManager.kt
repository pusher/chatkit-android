package com.pusher.chatkit

import com.pusher.chatkit.InstanceType.*
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.files.AttachmentBody
import com.pusher.chatkit.files.DataAttachment
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.util.parseAs
import com.pusher.chatkit.presence.PresenceService
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
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue


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

    internal inline fun <reified A> doPost(
        path: String,
        body: String = "",
        noinline responseParser: DataParser<A> = { it.parseAs() },
        instanceType: InstanceType = DEFAULT
    ): Future<Result<A, Error>> =
        doRequest("POST", path, body, responseParser, instanceType)

    internal inline fun <reified A> doPut(
        path: String,
        body: String = "",
        noinline responseParser: DataParser<A> = { it.parseAs() },
        instanceType: InstanceType = DEFAULT
    ): Future<Result<A, Error>> =
        doRequest("PUT", path, body, responseParser, instanceType)

    internal inline fun <reified A> doGet(
        path: String,
        noinline responseParser: DataParser<A> = { it.parseAs() },
        instanceType: InstanceType = DEFAULT
    ): Future<Result<A, Error>> =
        doRequest("GET", path, null, responseParser, instanceType)

    internal inline fun <reified A> doDelete(
        path: String,
        noinline responseParser: DataParser<A> = { it.parseAs() },
        instanceType: InstanceType = DEFAULT
    ): Future<Result<A, Error>> =
        doRequest("DELETE", path, null, responseParser, instanceType)

    private fun <A> doRequest(
        method: String,
        path: String,
        body: String?,
        responseParser: DataParser<A>,
        instanceType: InstanceType = DEFAULT
    ): Future<Result<A, Error>> =
        platformInstance(instanceType).request(
            options = RequestOptions(
                method = method,
                path = path,
                body = body
            ),
            tokenProvider = tokenProvider,
            responseParser = responseParser
        )

    @Suppress("UNCHECKED_CAST")
    internal fun upload(
        path: String,
        attachment: DataAttachment
    ): Future<Result<AttachmentBody, Error>> = platformInstance(FILES).upload(
        path = path,
        file = attachment.file,
        tokenProvider = tokenProvider,
        responseParser = { it.parseAs<AttachmentBody.Resource>() as Result<AttachmentBody, Error> }
    )

    internal fun <A> subscribeResuming(
        path: String,
        listeners: SubscriptionListeners<A>,
        messageParser: DataParser<A>,
        instanceType: InstanceType = DEFAULT
    ) = platformInstance(instanceType).subscribeResuming(
        path = path,
        tokenProvider = tokenProvider,
        listeners = listeners,
        messageParser = messageParser
    ).also { subscriptions += it }

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

    private val instances = mutableMapOf<InstanceType, Instance>()

    internal fun platformInstance(type: InstanceType = DEFAULT) =
        instances[type] ?: createInstance(type.serviceName, type.version).also { instances[type] = it }

    private fun createInstance(serviceName: String, serviceVersion: String): Instance {
        val instance = Instance(
            locator = instanceLocator,
            serviceName = serviceName,
            serviceVersion = serviceVersion,
            dependencies = dependencies
        )
        return dependencies.okHttpClient.let { client ->
            when (client) {
                null -> instance
                else -> instance.copy(baseClient = instance.baseClient.copy(client = client))
            }
        }
    }

}

internal enum class InstanceType(val serviceName: String, val version: String = "v1") {
    DEFAULT("chatkit"),
    CURSORS("chatkit_cursors"),
    PRESENCE("chatkit_presence"),
    FILES("chatkit_files")
}

