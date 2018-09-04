package com.pusher.chatkit

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.memberships.MembershipService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.presence.PresenceSubscriptionEvent
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.platform.Instance
import com.pusher.platform.network.*
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit

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

    internal val cursorService by lazy {
        CursorService(
                createPlatformClient(InstanceType.CURSORS),
                dependencies.logger
        )
    }

    private val presenceService by lazy {
        PresenceService(
                createPlatformClient(InstanceType.PRESENCE),
                userId,
                this::consumePresenceSubscriptionEvent,
                userService,
                dependencies.logger
        )
    }

    internal val userService by lazy {
        UserService(
                userId,
                this::consumeUserSubscriptionEvent,
                createPlatformClient(InstanceType.DEFAULT),
                roomService,
                dependencies.logger
        )
    }

    internal val messageService by lazy { MessageService(this) }
    internal val filesService by lazy { FilesService(this) }
    internal val roomService by lazy { RoomService(this) }
    internal val membershipService by lazy { MembershipService(this) }

    private var currentUser: CurrentUser? = null

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}): Future<Result<CurrentUser, Error>> {
        val futureCurrentUser = CurrentUserConsumer() // TODO: not sure about this!
        eventConsumers += futureCurrentUser
        eventConsumers += replaceCurrentUser
        eventConsumers += consumer

        // TODO: These each block, but they're supposed to happen in parallel
        userService.subscribe()
        presenceService.subscribe()
        cursorService.subscribeForUser(userId, this::consumeCursorSubscriptionEvent)

        return futureCurrentUser.get()
    }

    private val replaceCurrentUser = { event: ChatManagerEvent ->
        if (event is ChatManagerEvent.CurrentUserReceived) {
            this.currentUser?.apply {
                close()
                updateWithPropertiesOf(event.currentUser)
            }
        }
    }

    private fun consumeUserSubscriptionEvent(event: UserSubscriptionEvent) {
        transformUserSubscriptionEvent(event)
                .waitOr(Wait.For(10, TimeUnit.SECONDS)) { ChatManagerEvent.ErrorOccurred(Errors.other(it)).asSuccess() }
                .recover { ChatManagerEvent.ErrorOccurred(it) }
                .also { chatManagerEvent ->
                    eventConsumers.forEach { consumer ->
                        consumer(chatManagerEvent)
                    }
                }
    }

    private fun consumeCursorSubscriptionEvent(event: CursorSubscriptionEvent) {
        transformCursorSubscriptionEvent(event).also { chatManagerEvent ->
            eventConsumers.forEach { consumer ->
                consumer(chatManagerEvent)
            }
        }
    }

    private fun consumePresenceSubscriptionEvent(event: PresenceSubscriptionEvent) {
        transformPresenceSubscriptionEvent(event).also { chatManagerEvent ->
            eventConsumers.forEach { consumer ->
                consumer(chatManagerEvent)
            }
        }
    }

    private fun transformPresenceSubscriptionEvent(event: PresenceSubscriptionEvent): Future<ChatManagerEvent> =
            userService.fetchUserBy(event.userId)
                    .mapResult { user ->
                        user.takeIf { it.presence != event.presence }
                                ?.also { it.presence = event.presence }
                                ?.presence
                                .let { userPresence ->
                                    when (userPresence) {
                                        Presence.Online -> ChatManagerEvent.UserCameOnline(user)
                                        Presence.Offline -> ChatManagerEvent.UserWentOffline(user)
                                        null -> ChatManagerEvent.NoEvent
                                    }
                                }
                    }
                    .map { it.recover { error -> ChatManagerEvent.ErrorOccurred(error) } }

    private fun transformCursorSubscriptionEvent(event: CursorSubscriptionEvent): ChatManagerEvent =
            when (event) {
                is CursorSubscriptionEvent.OnCursorSet ->
                    ChatManagerEvent.NewReadCursor(event.cursor)
                else ->
                    ChatManagerEvent.NoEvent
            }

    private fun transformUserSubscriptionEvent(event: UserSubscriptionEvent): Future<Result<ChatManagerEvent, Error>> =
            when (event) {
                is UserSubscriptionEvent.InitialState -> getCursors().mapResult { cursors ->
                    cursorService.saveCursors(cursors)
                    ChatManagerEvent.CurrentUserReceived(createCurrentUser(event))
                }
                is UserSubscriptionEvent.AddedToRoomEvent -> ChatManagerEvent.CurrentUserAddedToRoom(event.room).toFutureSuccess()
                is UserSubscriptionEvent.RoomUpdatedEvent -> ChatManagerEvent.RoomUpdated(event.room).toFutureSuccess()
                is UserSubscriptionEvent.RoomDeletedEvent -> ChatManagerEvent.RoomDeleted(event.roomId).toFutureSuccess()
                is UserSubscriptionEvent.RemovedFromRoomEvent -> ChatManagerEvent.CurrentUserRemovedFromRoom(event.roomId).toFutureSuccess()
                is UserSubscriptionEvent.LeftRoomEvent -> userService.fetchUserBy(event.userId).flatMapResult { user ->
                    roomService.roomStore[event.roomId]
                            .orElse { Errors.other("room ${event.roomId} not found.") }
                            .map<ChatManagerEvent> { room -> ChatManagerEvent.UserLeftRoom(user, room) }
                }
                is UserSubscriptionEvent.JoinedRoomEvent -> userService.fetchUserBy(event.userId).flatMapResult { user ->
                    roomService.roomStore[event.roomId]
                            .orElse { Errors.other("room ${event.roomId} not found.") }
                            .map<ChatManagerEvent> { room -> ChatManagerEvent.UserJoinedRoom(user, room) }
                }
                is UserSubscriptionEvent.StartedTyping -> userService.fetchUserBy(event.userId).flatMapFutureResult { user ->
                    roomService.fetchRoomBy(user.id, event.roomId).mapResult { room ->
                        ChatManagerEvent.UserStartedTyping(user, room) as ChatManagerEvent
                    }
                }
                is UserSubscriptionEvent.StoppedTyping -> userService.fetchUserBy(event.userId).flatMapFutureResult { user ->
                    roomService.fetchRoomBy(user.id, event.roomId).mapResult { room ->
                        ChatManagerEvent.UserStoppedTyping(user, room) as ChatManagerEvent
                    }
                }
                is UserSubscriptionEvent.ErrorOccurred -> ChatManagerEvent.ErrorOccurred(event.error).toFutureSuccess()
            }

    private fun ChatManagerEvent.toFutureSuccess() =
            asSuccess<ChatManagerEvent, Error>().toFuture()

    private fun getCursors(): Future<Result<Map<Int, Cursor>, Error>> =
            cursorService.request(userId)
                    .mapResult { cursors ->
                        cursors.map { cursor ->
                            cursor.roomId to cursor
                        }.toMap()
                    }

    private fun createCurrentUser(initialState: UserSubscriptionEvent.InitialState) = CurrentUser(
            id = initialState.currentUser.id,
            avatarURL = initialState.currentUser.avatarURL,
            customData = initialState.currentUser.customData,
            name = initialState.currentUser.name,
            chatManager = this
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

    /**
     * Tries to close all pending subscriptions and resources
     */
    fun close(): Result<Unit, Error> = try {
        for (sub in subscriptions) {
            sub.unsubscribe()
        }
        userService.unsubscribe()
        presenceService.unsubscribe()
        cursorService.unsubscribe()
        dependencies.okHttpClient?.connectionPool()?.evictAll()
        eventConsumers.clear()
        Unit.asSuccess()
    } catch (e: Throwable) {
        Errors.other(e).asFailure()
    }

    private fun createPlatformClient(type: InstanceType) =
            PlatformClient(createInstance(type), tokenProvider)

    private fun createInstance(type: InstanceType): Instance {
        val instance = Instance(
            locator = instanceLocator,
            serviceName = type.serviceName,
            serviceVersion = type.version,
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
    DEFAULT("chatkit", "v2"),
    CURSORS("chatkit_cursors"),
    PRESENCE("chatkit_presence"),
    FILES("chatkit_files")
}
