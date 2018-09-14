package com.pusher.chatkit

import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.presence.PresenceSubscriptionEvent
import com.pusher.chatkit.presence.PresenceSubscriptionEventParser
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

class ChatManager constructor(
    private val instanceLocator: String,
    private val userId: String,
    internal val dependencies: ChatkitDependencies
) {
    private val tokenProvider: TokenProvider = DebounceTokenProvider(
            dependencies.tokenProvider.also { (it as? ChatkitTokenProvider)?.userId = userId }
    )

    private val logger = dependencies.logger
    private val chatkitClient = createPlatformClient(InstanceType.DEFAULT)
    private val cursorsClient = createPlatformClient(InstanceType.CURSORS)
    private val presenceClient = createPlatformClient(InstanceType.PRESENCE)
    private val filesClient = createPlatformClient(InstanceType.FILES)

    internal val cursorService = CursorService(cursorsClient, logger)
    internal val userService = UserService(chatkitClient)
    internal val filesService = FilesService(filesClient)
    internal val messageService = MessageService(chatkitClient, userService, filesService)

    private val eventConsumers = mutableListOf<ChatManagerEventConsumer>()

    internal val roomService =
        RoomService(
                chatkitClient,
                userService,
                cursorService,
                this.eventConsumers,
                this::consumeRoomSubscriptionEvent,
                dependencies.logger
        )

    private val presenceSubscription by lazy {
        ResolvableSubscription(
                client = presenceClient,
                path = "/users/$userId/presence",
                listeners = SubscriptionListeners(
                        onEvent = { consumePresenceSubscriptionEvent(it.body) },
                        onError = { error -> consumePresenceSubscriptionEvent(PresenceSubscriptionEvent.ErrorOccurred(error)) }
                ),
                messageParser = PresenceSubscriptionEventParser,
                logger = logger,
                description = "Presence for user $userId",
                resolveOnFirstEvent = true
        )
    }

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
        private var currentUser: CurrentUser? = null

        fun get(): CurrentUser {
            latch.await()
            return currentUser!!
        }

        fun set(e: CurrentUser) {
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

    fun connect(listeners: ChatManagerListeners): Result<CurrentUser, Error> =
            connect(listeners.toCallback())

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}): Result<CurrentUser, Error> {
        eventConsumers += consumer

        // Touching them constructs them. Lazy is weird
        userSubscription
        presenceSubscription
        cursorSubscription
        // Then we await the connection of all three
        userSubscription.await()
        presenceSubscription.await()
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

    private fun consumeRoomSubscriptionEvent(roomId: Int): RoomConsumer = { event ->
                consumeEvents(listOf(transformRoomSubscriptionEvent(roomId, event)))
            }

    private fun consumePresenceSubscriptionEvent(event: PresenceSubscriptionEvent) =
            consumeEvents(transformPresenceSubscriptionEvent(event))

    private fun consumeEvents(events : List<ChatManagerEvent>) {
        events.filter { event ->
            event !is ChatManagerEvent.NoEvent
        }.forEach { event ->
            eventConsumers.forEach { consumer ->
                consumer(event)
            }
        }
    }

    private fun transformRoomSubscriptionEvent(roomId: Int, event: RoomEvent): ChatManagerEvent =
        when (event) {
            is RoomEvent.UserStartedTyping ->
                roomService.fetchRoomBy(event.user.id, roomId).map { room ->
                    ChatManagerEvent.UserStartedTyping(event.user, room) as ChatManagerEvent
                }.recover { ChatManagerEvent.ErrorOccurred(it) }
            is RoomEvent.UserStoppedTyping ->
                roomService.fetchRoomBy(event.user.id, roomId).map { room ->
                    ChatManagerEvent.UserStoppedTyping(event.user, room) as ChatManagerEvent
                }.recover { ChatManagerEvent.ErrorOccurred(it) }
            else ->
                ChatManagerEvent.NoEvent
        }

    private fun transformPresenceSubscriptionEvent(event: PresenceSubscriptionEvent): List<ChatManagerEvent> {
        val newStates = when (event) {
            is PresenceSubscriptionEvent.InitialState -> event.userStates
            is PresenceSubscriptionEvent.JoinedRoom -> event.userStates
            is PresenceSubscriptionEvent.PresenceUpdate -> listOf(event.state)
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
                user.presence = newState.presence

                when (newState.presence) {
                    is Presence.Online -> ChatManagerEvent.UserCameOnline(user)
                    is Presence.Offline -> ChatManagerEvent.UserWentOffline(user)
                }
            }
        }.recover {
            listOf(ChatManagerEvent.ErrorOccurred(it))
        }
    }

    private fun transformCursorsSubscriptionEvent(event: CursorSubscriptionEvent): ChatManagerEvent =
                when (event) {
                    is CursorSubscriptionEvent.OnCursorSet ->
                        ChatManagerEvent.NewReadCursor(event.cursor)
                    else ->
                        ChatManagerEvent.NoEvent
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

    private fun transformUserSubscriptionEvent(event: UserSubscriptionEvent): ChatManagerEvent =
            when (event) {
                is UserSubscriptionEvent.InitialState ->
                    ChatManagerEvent.CurrentUserReceived(currentUser.get())
                is UserSubscriptionEvent.AddedToRoomEvent ->
                    ChatManagerEvent.CurrentUserAddedToRoom(event.room)
                is UserSubscriptionEvent.RemovedFromRoomEvent ->
                    ChatManagerEvent.CurrentUserRemovedFromRoom(event.roomId)
                is UserSubscriptionEvent.RoomUpdatedEvent ->
                    ChatManagerEvent.RoomUpdated(event.room)
                is UserSubscriptionEvent.RoomDeletedEvent ->
                    ChatManagerEvent.RoomDeleted(event.roomId)
                is UserSubscriptionEvent.LeftRoomEvent ->
                            userService.fetchUserBy(event.userId).flatMap { user ->
                                roomService.roomStore[event.roomId]
                                        .orElse { Errors.other("room ${event.roomId} not found.") }
                                        .map { room -> ChatManagerEvent.UserLeftRoom(user, room) as ChatManagerEvent }
                            }.recover { ChatManagerEvent.ErrorOccurred(it) }
                is UserSubscriptionEvent.JoinedRoomEvent ->
                            userService.fetchUserBy(event.userId).flatMap { user ->
                                roomService.roomStore[event.roomId]
                                        .orElse { Errors.other("room ${event.roomId} not found.") }
                                        .map { room -> ChatManagerEvent.UserJoinedRoom(user, room) as ChatManagerEvent }
                            }.recover { ChatManagerEvent.ErrorOccurred(it) }
                is UserSubscriptionEvent.ErrorOccurred ->
                    ChatManagerEvent.ErrorOccurred(event.error)
            }

    private fun createCurrentUser(initialState: UserSubscriptionEvent.InitialState) = CurrentUser(
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
        presenceSubscription.unsubscribe()
        cursorSubscription.unsubscribe()
        roomService.close()
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
    CURSORS("chatkit_cursors"),
    PRESENCE("chatkit_presence"),
    FILES("chatkit_files")
}