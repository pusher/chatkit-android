package com.pusher.chatkit

import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.messages.multipart.UrlRefresher
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.presence.PresenceSubscriptionEvent
import com.pusher.chatkit.pushnotifications.BeamsTokenProviderService
import com.pusher.chatkit.rooms.RoomConsumer
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.users.UserSubscriptionEventParser
import com.pusher.chatkit.util.makeSafe
import com.pusher.platform.Instance
import com.pusher.platform.Locator
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import java.util.concurrent.CountDownLatch

class SynchronousChatManager constructor(
        private val instanceLocator: String,
        private val userId: String,
        private val dependencies: ChatkitDependencies
) : AppHookListener {
    private val tokenProvider: TokenProvider = DebounceTokenProvider(
            dependencies.tokenProvider.also { (it as? ChatkitTokenProvider)?.userId = userId }
    )

    private val logger = dependencies.logger
    private val v2chatkitClient = createPlatformClient(InstanceType.SERVER_V2)
    private val v3chatkitClient = createPlatformClient(InstanceType.SERVER_V4)
    private val cursorsClient = createPlatformClient(InstanceType.CURSORS)
    private val presenceClient = createPlatformClient(InstanceType.PRESENCE)
    private val filesClient = createPlatformClient(InstanceType.FILES)

    private val beams = dependencies.pushNotifications?.newBeams(
            Locator(instanceLocator).id,
            BeamsTokenProviderService(createPlatformClient(InstanceType.BEAMS_TOKEN_PROVIDER))
    )

    private val eventConsumers = mutableListOf<ChatManagerEventConsumer>()

    private val urlRefresher = UrlRefresher(v3chatkitClient)

    internal val cursorService = CursorService(cursorsClient, logger)

    private val filesService = FilesService(filesClient)

    private val presenceService =
            PresenceService(
                    myUserId = userId,
                    logger = logger,
                    client = presenceClient,
                    consumer = this::consumePresenceSubscriptionEvent
            )

    internal val userService = UserService(v3chatkitClient, presenceService)

    internal val roomService =
            RoomService(
                    v2chatkitClient,
                    v3chatkitClient,
                    urlRefresher,
                    userService,
                    cursorService,
                    this::consumeRoomSubscriptionEvent,
                    dependencies.logger
            )

    internal val messageService = MessageService(
            v2chatkitClient,
            v3chatkitClient,
            userService,
            roomService,
            urlRefresher,
            filesService
    )

    private lateinit var userSubscription: ResolvableSubscription<UserSubscriptionEvent>


    private var currentUser = object {
        private val updateLock = object {}
        private val latch = CountDownLatch(1)
        private var currentUser: SynchronousCurrentUser? = null

        fun get(): SynchronousCurrentUser {
            latch.await()
            return currentUser!!
        }

        fun set(e: SynchronousCurrentUser) {
            synchronized(updateLock) {
                if (currentUser == null) {
                    currentUser = e
                    latch.countDown()
                } else {
                    currentUser!!.updateWithPropertiesOf(e)
                }
            }
        }
    }

    // Holds events emitted during connection until we're initialised fully
    private val eventBuffer = object {
        private val buffer = ArrayList<ChatEvent>()
        private var released = false

        fun queue(event: ChatEvent) {
            synchronized(buffer) {
                if (released) {
                    emit(event)
                } else {
                    buffer.add(event)
                }
            }
        }

        private fun emit(event: ChatEvent) {
            eventConsumers.forEach { consumer ->
                consumer(event)
            }
        }

        fun release() {
            synchronized(buffer) {
                buffer.forEach { emit(it) }
                buffer.clear()
                released = true
            }
        }
    }

    fun connect(listeners: ChatListeners): Result<SynchronousCurrentUser, Error> =
            connect(listeners.toCallback())

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}): Result<SynchronousCurrentUser, Error> {
        eventConsumers += { event -> makeSafe(logger) { consumer(event) } }
        // The room service filters and translates some global events (e.g. RoomUpdated), forwarding
        // them to the relevant room subscriptions
        // The room service has ensured the safety of its own consumers.
        eventConsumers += roomService::distributeGlobalEvent

        dependencies.appHooks.register(this)

        userSubscription = ResolvableSubscription(
                client = v3chatkitClient,
                path = "users",
                listeners = SubscriptionListeners(
                        onEvent = { event -> consumeUserSubscriptionEvent(event.body) },
                        onError = { error -> consumeUserSubscriptionEvent(UserSubscriptionEvent.ErrorOccurred(error)) }
                ),
                messageParser = UserSubscriptionEventParser,
                resolveOnFirstEvent = true,
                description = "User $userId",
                logger = logger
        )
        userSubscription.await()

        return currentUser.get()
                .also { logger.verbose("Current User initialised") }
                .also { eventBuffer.release() }
                .asSuccess()
    }

    override fun onAppOpened() {
        presenceService.goOnline()
    }

    override fun onAppClosed() {
        presenceService.goOffline()
    }

    private fun consumeUserSubscriptionEvent(incomingEvent: UserSubscriptionEvent) {
        val appliedEvents = roomService.roomStore.applyUserSubscriptionEvent(incomingEvent) +
                cursorService.applyEvent(incomingEvent)

        return appliedEvents.forEach { event ->
            if (event is UserSubscriptionEvent.InitialState) {
                updateCurrentUser(event)
            }
            eventBuffer.queue(transformUserSubscriptionEvent(event))
        }
    }

    private fun consumeRoomSubscriptionEvent(roomId: String): RoomConsumer = { event ->
        consumeEvents(listOf(transformRoomSubscriptionEvent(roomId, event)))
    }

    private fun consumePresenceSubscriptionEvent(event: PresenceSubscriptionEvent) =
            consumeEvents(transformPresenceSubscriptionEvent(event))

    private fun consumeEvents(events: List<ChatEvent>) {
        events.forEach(eventBuffer::queue)
    }

    private fun transformUserSubscriptionEvent(event: UserSubscriptionEvent): ChatEvent =
            when (event) {
                is UserSubscriptionEvent.InitialState ->
                    ChatEvent.CurrentUserReceived(currentUser.get())
                is UserSubscriptionEvent.AddedToRoomEvent ->
                    ChatEvent.AddedToRoom(event.room)
                is UserSubscriptionEvent.RemovedFromRoomEvent ->
                    ChatEvent.RemovedFromRoom(event.roomId)
                is UserSubscriptionEvent.RoomUpdatedEvent ->
                    ChatEvent.RoomUpdated(event.room)
                is UserSubscriptionEvent.RoomDeletedEvent ->
                    ChatEvent.RoomDeleted(event.roomId)
                is UserSubscriptionEvent.NewCursor ->
                    ChatEvent.NewReadCursor(event.cursor)
                is UserSubscriptionEvent.ErrorOccurred ->
                    ChatEvent.ErrorOccurred(event.error)
            }

    private fun transformRoomSubscriptionEvent(roomId: String, event: RoomEvent): ChatEvent =
            when (event) {
                is RoomEvent.UserStartedTyping ->
                    roomService.fetchRoom(roomId).map { room ->
                        ChatEvent.UserStartedTyping(event.user, room) as ChatEvent
                    }.recover { ChatEvent.ErrorOccurred(it) }
                is RoomEvent.UserStoppedTyping ->
                    roomService.fetchRoom(roomId).map { room ->
                        ChatEvent.UserStoppedTyping(event.user, room) as ChatEvent
                    }.recover { ChatEvent.ErrorOccurred(it) }
                else ->
                    ChatEvent.NoEvent
            }

    private fun transformPresenceSubscriptionEvent(event: PresenceSubscriptionEvent): List<ChatEvent> {
        val newStates = when (event) {
            is PresenceSubscriptionEvent.PresenceUpdate -> listOf(event.presence)
            else -> listOf()
        }

        return newStates.map { newState ->
            newState.userId
        }.let { userIDs ->
            userService.fetchUsersBy(userIDs.toSet())
        }.map { users ->
            newStates.map { newState ->
                newState to users.getValue(newState.userId)
            }.filter { (newState, user) ->
                newState.presence != user.presence
            }.map { (newState, user) ->
                val oldState = user.presence
                user.presence = newState.presence

                when (newState.presence) {
                    is Presence.Online -> ChatEvent.PresenceChange(user, newState.presence, oldState)
                    is Presence.Offline -> ChatEvent.PresenceChange(user, newState.presence, oldState)
                    is Presence.Unknown -> ChatEvent.NoEvent
                }
            }
        }.recover {
            listOf(ChatEvent.ErrorOccurred(it))
        }
    }

    private fun updateCurrentUser(initialState: UserSubscriptionEvent.InitialState) {
        currentUser.set(SynchronousCurrentUser(
                id = initialState.currentUser.id,
                avatarURL = initialState.currentUser.avatarURL,
                customData = initialState.currentUser.customData,
                name = initialState.currentUser.name,
                chatManager = this,
                pushNotifications = beams,
                client = createPlatformClient(InstanceType.SERVER_V4)
        ))
    }

    /**
     * Tries to close all pending subscriptions and resources
     */
    fun close(): Result<Unit, Error> = try {
        dependencies.appHooks.unregister(this)
        userSubscription.unsubscribe()
        roomService.close()
        presenceService.close()
        cursorService.close()
        dependencies.okHttpClient?.connectionPool()?.evictAll()
        eventConsumers.clear()

        Unit.asSuccess()
    } catch (e: Throwable) {
        Errors.other(e).asFailure()
    }

    private fun createPlatformClient(type: InstanceType): PlatformClient {
        val instance = Instance(
                locator = instanceLocator,
                serviceName = type.serviceName,
                serviceVersion = type.version,
                dependencies = dependencies
        )
        return PlatformClient(dependencies.okHttpClient.let { client ->
            when (client) {
                null -> instance
                else -> instance.copy(baseClient = instance.baseClient.copy(client = client))
            }
        }, tokenProvider)
    }

    fun disablePushNotifications(): Result<Unit, Error> {
        if (beams != null) {
            return beams.stop()
        } else {
            throw IllegalStateException("Push Notifications dependency is not available. " +
                    "Did you provide a Context to AndroidChatkitDependencies?")
        }
    }
}

internal enum class InstanceType(val serviceName: String, val version: String = "v1") {
    SERVER_V2("chatkit", "v2"),
    SERVER_V4("chatkit", "v4"),
    CURSORS("chatkit_cursors", "v2"),
    PRESENCE("chatkit_presence", "v2"),
    FILES("chatkit_files"),
    BEAMS_TOKEN_PROVIDER("chatkit_beams_token_provider")
}
