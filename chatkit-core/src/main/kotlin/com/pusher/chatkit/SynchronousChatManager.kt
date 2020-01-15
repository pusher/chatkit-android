package com.pusher.chatkit

import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.messages.multipart.UrlRefresher
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.presence.PresenceSubscriptionEvent
import com.pusher.chatkit.pushnotifications.BeamsTokenProviderService
import com.pusher.chatkit.pushnotifications.PushNotifications
import com.pusher.chatkit.rooms.RoomConsumer
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.UserSubscription
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.util.makeSafe
import com.pusher.platform.Instance
import com.pusher.platform.Locator
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors

class SynchronousChatManager : AppHookListener {

    constructor(
            instanceLocator: String,
            userId: String,
            dependencies: ChatkitDependencies
    ) : this(instanceLocator, userId, dependencies, DefaultPlatformClientFactory())

    internal constructor(
            instanceLocator: String,
            userId: String,
            dependencies: ChatkitDependencies,
            platformClientFactory: PlatformClientFactory
    ) {
        this.instanceLocator = instanceLocator
        this.userId = userId
        this.dependencies = dependencies
        this.platformClientFactory = platformClientFactory

        tokenProvider = DebounceTokenProvider(
                dependencies.tokenProvider
                        .also { (it as? ChatkitTokenProvider)?.userId = userId }
        )

        logger = dependencies.logger

        coreLegacyV2Client = createPlatformClient(InstanceType.CORE_LEGACY_V2)
        coreClient = createPlatformClient(InstanceType.CORE)
        cursorsClient = createPlatformClient(InstanceType.CURSORS)
        presenceClient = createPlatformClient(InstanceType.PRESENCE)
        filesClient = createPlatformClient(InstanceType.FILES)

        beams = dependencies.pushNotifications?.newBeams(
                Locator(instanceLocator).id,
                BeamsTokenProviderService(createPlatformClient(InstanceType.BEAMS_TOKEN_PROVIDER))
        )

        urlRefresher = UrlRefresher(coreClient)

        cursorService = CursorService(cursorsClient, logger)
        filesService = FilesService(filesClient)
        presenceService =
                PresenceService(
                        myUserId = userId,
                        logger = logger,
                        client = presenceClient,
                        consumer = this::consumePresenceSubscriptionEvent
                )
        userService = UserService(coreClient, presenceService)
        roomService =
                RoomService(
                        coreLegacyV2Client,
                        coreClient,
                        urlRefresher,
                        userService,
                        cursorService,
                        this::consumeRoomSubscriptionEvent,
                        dependencies.logger
                )
        messageService = MessageService(
                coreLegacyV2Client,
                coreClient,
                userService,
                roomService,
                urlRefresher,
                filesService
        )
    }

    private val instanceLocator: String
    private val userId: String
    private val dependencies: ChatkitDependencies
    private val platformClientFactory: PlatformClientFactory

    private val tokenProvider: TokenProvider

    private val logger: Logger

    private val coreLegacyV2Client: PlatformClient
    private val coreClient: PlatformClient
    private val cursorsClient: PlatformClient
    private val presenceClient: PlatformClient
    private val filesClient: PlatformClient

    private val beams: PushNotifications?

    private val eventConsumers = mutableListOf<ChatManagerEventConsumer>()

    private val urlRefresher: UrlRefresher

    internal val cursorService: CursorService
    private val filesService: FilesService
    private val presenceService: PresenceService
    internal val userService: UserService
    internal val roomService: RoomService
    internal val messageService: MessageService

    private lateinit var userSubscription: UserSubscription
    private lateinit var currentUser: SynchronousCurrentUser

    private fun emit(event: ChatEvent) {
        eventConsumers.forEach { consumer ->
            consumer(event)
        }
    }

    private val populatedInitialStateLock = Object()
    private var populatedInitialState = false

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

        userSubscription = UserSubscription(
                userId = userId,
                client = coreClient,
                listeners = ::consumeUserSubscriptionEvent,
                logger = logger
        )

        return userSubscription.initialState().map { initialState ->
            currentUser = newCurrentUser(initialState)
            logger.verbose("Current User initialised")
            roomService.populateInitial(initialState.rooms)
            cursorService.populateInitial(initialState.cursors)
            emit(ChatEvent.CurrentUserReceived(currentUser))

            synchronized(populatedInitialStateLock) {
                populatedInitialState = true
                populatedInitialStateLock.notify() // there should be only one thread waiting
            }

            currentUser
        }
    }

    override fun onAppOpened() {
        presenceService.goOnline()
    }

    override fun onAppClosed() {
        presenceService.goOffline()
    }

    private fun consumeUserSubscriptionEvent(incomingEvent: UserSubscriptionEvent) {
        synchronized(populatedInitialStateLock) {  // wait for initial state to be processed first
            if (!populatedInitialState) populatedInitialStateLock.wait()
        }

        val appliedEvents = roomService.roomStore.applyUserSubscriptionEvent(incomingEvent) +
                cursorService.applyEvent(incomingEvent)

        return appliedEvents.forEach { event ->
            if (event is UserSubscriptionEvent.InitialState) {
                currentUser.updateWithPropertiesOf(newCurrentUser(event))
            }
            emit(transformUserSubscriptionEvent(event))
        }
    }

    private fun consumeRoomSubscriptionEvent(roomId: String): RoomConsumer = { event ->
        consumeEvents(listOf(transformRoomSubscriptionEvent(roomId, event)))
    }

    private fun consumePresenceSubscriptionEvent(event: PresenceSubscriptionEvent) =
            consumeEvents(transformPresenceSubscriptionEvent(event))

    private fun consumeEvents(events: List<ChatEvent>) {
        events.forEach(this::emit)
    }

    private fun transformUserSubscriptionEvent(event: UserSubscriptionEvent): ChatEvent =
            when (event) {
                is UserSubscriptionEvent.InitialState ->
                    ChatEvent.NoEvent // This is emitted specially on connect
                is UserSubscriptionEvent.AddedToRoomApiEvent ->
                    ChatEvent.AddedToRoom(event.room)
                is UserSubscriptionEvent.AddedToRoomEvent -> // reconnect
                    ChatEvent.AddedToRoom(event.room)
                is UserSubscriptionEvent.RemovedFromRoomEvent ->
                    ChatEvent.RemovedFromRoom(event.roomId)
                is UserSubscriptionEvent.RoomUpdatedEvent ->
                    ChatEvent.RoomUpdated(event.room)
                is UserSubscriptionEvent.RoomDeletedEvent ->
                    ChatEvent.RoomDeleted(event.roomId)
                is UserSubscriptionEvent.ReadStateUpdatedEvent -> {
                    val receivedCursor = event.readState.cursor
                    if (receivedCursor != null) {
                        ChatEvent.NewReadCursor(receivedCursor)
                    } else {
                        ChatEvent.RoomUpdated(roomService.roomStore[event.readState.roomId]!!)
                    }
                }
                is UserSubscriptionEvent.UserJoinedRoomEvent ->
                    userService.fetchUserBy(event.userId).fold(
                            onSuccess = { user ->
                                val room = roomService.roomStore[event.roomId]!!
                                ChatEvent.UserJoinedRoom(user, room)
                            },
                            onFailure = { ChatEvent.ErrorOccurred(it) }
                    )
                is UserSubscriptionEvent.UserLeftRoomEvent ->
                    userService.fetchUserBy(event.userId).fold(
                            onSuccess = { user ->
                                val room = roomService.roomStore[event.roomId]!!
                                ChatEvent.UserLeftRoom(user, room)
                            },
                            onFailure = { ChatEvent.ErrorOccurred(it) }
                    )
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

    private fun newCurrentUser(initialState: UserSubscriptionEvent.InitialState) =
        SynchronousCurrentUser(
                id = initialState.currentUser.id,
                avatarURL = initialState.currentUser.avatarURL,
                customData = initialState.currentUser.customData,
                name = initialState.currentUser.name,
                chatManager = this,
                pushNotifications = beams,
                client = createPlatformClient(InstanceType.CORE)
        )

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
        return platformClientFactory.createPlatformClient(dependencies.okHttpClient.let { client ->
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
    CORE_LEGACY_V2("chatkit", "v2"),
    CORE("chatkit", "v7"),
    CURSORS("chatkit_cursors", "v2"),
    PRESENCE("chatkit_presence", "v2"),
    FILES("chatkit_files"),
    BEAMS_TOKEN_PROVIDER("chatkit_beams_token_provider")
}
