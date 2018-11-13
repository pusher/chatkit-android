package com.pusher.chatkit

import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.presence.PresenceSubscriptionEvent
import com.pusher.chatkit.rooms.RoomConsumer
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.users.UserSubscriptionEventParser
import com.pusher.platform.Instance
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
    internal val dependencies: ChatkitDependencies
) {
    private val tokenProvider: TokenProvider = DebounceTokenProvider(
            dependencies.tokenProvider.also { (it as? ChatkitTokenProvider)?.queryParams?.put("user_id", userId) }
    )

    private val logger = dependencies.logger
    private val chatkitClient = createPlatformClient(InstanceType.DEFAULT)
    private val cursorsClient = createPlatformClient(InstanceType.CURSORS)
    private val presenceClient = createPlatformClient(InstanceType.PRESENCE)
    private val filesClient = createPlatformClient(InstanceType.FILES)

    private val eventConsumers = mutableListOf<ChatManagerEventConsumer>()

    internal val cursorService = CursorService(cursorsClient, logger)

    internal val filesService = FilesService(filesClient)

    private val presenceService =
            PresenceService(
                    myUserId = userId,
                    logger = logger,
                    client = presenceClient,
                    consumer = this::consumePresenceSubscriptionEvent
            )

    internal val userService = UserService(chatkitClient, presenceService)

    internal val messageService = MessageService(chatkitClient, userService, filesService)

    internal val roomService =
            RoomService(
                    chatkitClient,
                    userService,
                    cursorService,
                    this.eventConsumers,
                    this::consumeRoomSubscriptionEvent,
                    dependencies.logger
            )

    private val userSubscription by lazy {
        ResolvableSubscription(
                client = chatkitClient,
                path = "users",
                listeners = SubscriptionListeners(
                        onEvent = { event -> consumeUserSubscriptionEvent(event.body) },
                        onError = { error -> consumeUserSubscriptionEvent(UserSubscriptionEvent.ErrorOccurred(error)) }
                ),
                messageParser = UserSubscriptionEventParser,
                resolveOnFirstEvent = true,
                description = "User",
                logger = logger
        )
    }

    private val cursorSubscription by lazy {
        cursorService.subscribeForUser(userId, this::consumeCursorSubscriptionEvent)
    }

    private var currentUser = object {
        private val updateLock = object{}
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

    fun connect(listeners: ChatListeners): Result<SynchronousCurrentUser, Error> =
            connect(listeners.toCallback())

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}): Result<SynchronousCurrentUser, Error> {
        eventConsumers += consumer

        // Touching them constructs them. Lazy is weird
        userSubscription
        cursorSubscription
        // Then we await the connection of both
        userSubscription.await()
        cursorSubscription.await()

        return currentUser.get()
                .also { logger.verbose("Current User initialised") }
                .asSuccess()
    }

    private fun consumeUserSubscriptionEvent(event: UserSubscriptionEvent) =
            consumeEvents(
                    applyUserSubscriptionEvent(event).map { transformUserSubscriptionEvent(it) }
            )

    private fun consumeCursorSubscriptionEvent(event: CursorSubscriptionEvent) =
            consumeEvents(listOf(transformCursorsSubscriptionEvent(event)))

    private fun consumeRoomSubscriptionEvent(roomId: String): RoomConsumer = { event ->
                consumeEvents(listOf(transformRoomSubscriptionEvent(roomId, event)))
            }

    private fun consumePresenceSubscriptionEvent(event: PresenceSubscriptionEvent) =
            consumeEvents(transformPresenceSubscriptionEvent(event))

    private fun consumeEvents(events : List<ChatEvent>) {
        events.filter { event ->
            event !is ChatEvent.NoEvent
        }.forEach { event ->
            eventConsumers.forEach { consumer ->
                consumer(event)
            }
        }
    }

    private fun transformRoomSubscriptionEvent(roomId: String, event: RoomEvent): ChatEvent =
        when (event) {
            is RoomEvent.UserStartedTyping ->
                roomService.fetchRoomBy(event.user.id, roomId).map { room ->
                    ChatEvent.UserStartedTyping(event.user, room) as ChatEvent
                }.recover { ChatEvent.ErrorOccurred(it) }
            is RoomEvent.UserStoppedTyping ->
                roomService.fetchRoomBy(event.user.id, roomId).map { room ->
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

    private fun transformCursorsSubscriptionEvent(event: CursorSubscriptionEvent): ChatEvent =
                when (event) {
                    is CursorSubscriptionEvent.OnCursorSet ->
                        ChatEvent.NewReadCursor(event.cursor)
                    else ->
                        ChatEvent.NoEvent
                }

    private fun applyUserSubscriptionEvent(event: UserSubscriptionEvent): List<UserSubscriptionEvent> =
        when (event) {
            is UserSubscriptionEvent.InitialState -> {
                val removedFrom = (roomService.roomStore.toList() - event.rooms).also {
                    roomService.roomStore -= it
                }.map {
                    UserSubscriptionEvent.RemovedFromRoomEvent(it.id)
                }

                val addedTo = (event.rooms - roomService.roomStore.toList()).also {
                    roomService.roomStore += it
                }.map {
                    UserSubscriptionEvent.AddedToRoomEvent(it)
                }

                currentUser.set(createCurrentUser(event))

                removedFrom + addedTo + listOf(event)
            }
            is UserSubscriptionEvent.AddedToRoomEvent ->
                listOf(event.also { roomService.roomStore += event.room })
            is UserSubscriptionEvent.RoomUpdatedEvent ->
                listOf(event.also { roomService.roomStore += event.room })
            is UserSubscriptionEvent.RoomDeletedEvent ->
                listOf(event.also { roomService.roomStore -= event.roomId })
            is UserSubscriptionEvent.RemovedFromRoomEvent ->
                listOf(event.also { roomService.roomStore -= event.roomId })
            is UserSubscriptionEvent.LeftRoomEvent ->
                listOf(event.also { roomService.roomStore[event.roomId]?.removeUser(event.userId) })
            is UserSubscriptionEvent.JoinedRoomEvent ->
                listOf(event.also { roomService.roomStore[event.roomId]?.addUser(event.userId) })
            else -> listOf(event)
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
                is UserSubscriptionEvent.LeftRoomEvent ->
                            userService.fetchUserBy(event.userId).flatMap { user ->
                                roomService.roomStore[event.roomId]
                                        .orElse { Errors.other("room ${event.roomId} not found.") }
                                        .map { room -> ChatEvent.UserLeftRoom(user, room) as ChatEvent }
                            }.recover { ChatEvent.ErrorOccurred(it) }
                is UserSubscriptionEvent.JoinedRoomEvent ->
                            userService.fetchUserBy(event.userId).flatMap { user ->
                                roomService.roomStore[event.roomId]
                                        .orElse { Errors.other("room ${event.roomId} not found.") }
                                        .map { room -> ChatEvent.UserJoinedRoom(user, room) as ChatEvent }
                            }.recover { ChatEvent.ErrorOccurred(it) }
                is UserSubscriptionEvent.ErrorOccurred ->
                    ChatEvent.ErrorOccurred(event.error)
            }

    private fun createCurrentUser(initialState: UserSubscriptionEvent.InitialState) = SynchronousCurrentUser(
            id = initialState.currentUser.id,
            avatarURL = initialState.currentUser.avatarURL,
            customData = initialState.currentUser.customData,
            name = initialState.currentUser.name,
            chatManager = this,
            client = createPlatformClient(InstanceType.DEFAULT)
    )

    /**
     * Tries to close all pending subscriptions and resources
     */
    fun close(): Result<Unit, Error> = try {
        userSubscription.unsubscribe()
        cursorSubscription.unsubscribe()
        roomService.close()
        presenceService.close()
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
}

internal enum class InstanceType(val serviceName: String, val version: String = "v1") {
    DEFAULT("chatkit", "v2"),
    CURSORS("chatkit_cursors", "v2"),
    PRESENCE("chatkit_presence", "v2"),
    FILES("chatkit_files")
}
