package com.pusher.chatkit

import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.files.FilesService
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.presence.PresenceSubscription
import com.pusher.chatkit.presence.PresenceSubscriptionEvent
import com.pusher.chatkit.rooms.RoomConsumer
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.UserSubscription
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.platform.Instance
import com.pusher.platform.network.Futures
import com.pusher.platform.network.Wait
import com.pusher.platform.network.waitOr
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.Error
import elements.Errors
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

    private val eventConsumers = mutableListOf<ChatManagerEventConsumer>()

    internal val cursorService by lazy {
        CursorService(
                createPlatformClient(InstanceType.CURSORS),
                dependencies.logger
        )
    }

    private val presenceSubscription by lazy {
        PresenceSubscription(
                createPlatformClient(InstanceType.PRESENCE),
                userId,
                this::consumePresenceSubscriptionEvent,
                dependencies.logger
        )
    }

    private val cursorSubscription by lazy {
        cursorService.subscribeForUser(userId, this::consumeCursorSubscriptionEvent)
    }

    internal val userService by lazy {
        UserService(
                createPlatformClient(InstanceType.DEFAULT)
        )
    }

    private val userSubscription by lazy {
        UserSubscription(
                createPlatformClient(InstanceType.DEFAULT),
                this::consumeUserSubscriptionEvent,
                dependencies.logger
        )
    }

    internal val messageService by lazy {
        MessageService(
                createPlatformClient(InstanceType.DEFAULT),
                userService,
                filesService
        )
    }

    internal val filesService by lazy {
        FilesService(createPlatformClient(InstanceType.FILES))
    }

    internal val roomService by lazy {
        RoomService(
                this,
                createPlatformClient(InstanceType.DEFAULT),
                userService,
                cursorService,
                this::consumeRoomSubscriptionEvent,
                dependencies.logger
        )
    }

    private var currentUser: CurrentUser? = null

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}): Future<Result<CurrentUser, Error>> {
        val futureCurrentUser = CurrentUserConsumer() // TODO: not sure about this!
        eventConsumers += futureCurrentUser
        eventConsumers += replaceCurrentUser
        eventConsumers += consumer

        // TODO: These each block, but they're supposed to happen in parallel
        userSubscription.connect()
        presenceSubscription.connect()
        cursorSubscription.connect()

        return futureCurrentUser.get().also { dependencies.logger.verbose("Current User initialised") }
    }

    private val replaceCurrentUser = { event: ChatManagerEvent ->
        if (event is ChatManagerEvent.CurrentUserReceived) {
            this.currentUser?.apply {
                close()
                updateWithPropertiesOf(event.currentUser)
            }
        }
    }

    private fun consumeUserSubscriptionEvent(event: UserSubscriptionEvent) =
            consumeEvents(
                    applyUserSubscriptionEvent(event).map { transformUserSubscriptionEvent(it) }
            )

    private fun consumeCursorSubscriptionEvent(event: CursorSubscriptionEvent) =
            consumeEvents(listOf(transformCursorsSubscriptionEvent(event)))

    private fun consumeRoomSubscriptionEvent(roomId: Int): RoomConsumer =
            { event ->
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
                roomService.fetchRoomBy(event.user.id, roomId).mapResult { room ->
                    ChatManagerEvent.UserStartedTyping(event.user, room) as ChatManagerEvent
                }.waitAndRecover()
            is RoomEvent.UserStoppedTyping ->
                roomService.fetchRoomBy(event.user.id, roomId).mapResult { room ->
                    ChatManagerEvent.UserStoppedTyping(event.user, room) as ChatManagerEvent
                }.waitAndRecover()
            else ->
                ChatManagerEvent.NoEvent
        }

    private fun transformPresenceSubscriptionEvent(event: PresenceSubscriptionEvent): List<ChatManagerEvent> =
            when (event) {
                is PresenceSubscriptionEvent.InitialState -> event.userStates
                is PresenceSubscriptionEvent.JoinedRoom -> event.userStates
                is PresenceSubscriptionEvent.PresenceUpdate -> listOf(event.state)
                else -> listOf()
            }.map { newState ->
                // TODO we should be making use of the userService.fetchUser*s*By() method in order
                // to fetch all users in one call to the server, and enforce a maximum wait of 10 seconds.
                newState to userService.fetchUserBy(newState.userId)
            }.map { (newState, futureUserResult) ->
                // Unwrap the futures in a separate stage, so that they are
                // all initiated before we wait for any to complete
                newState to futureUserResult.waitOr(Wait.For(10, TimeUnit.SECONDS)) {
                    Errors.other(it).asFailure()
                }
            }.filter { (newState, userResult) ->
                when (userResult) {
                    is Result.Success ->
                        userResult.value.presence != newState.presence
                    is Result.Failure ->
                        true // don't filter out failures, we need to report them
                }
            }.map { (newState, userResult) ->
                userResult
                        .map { user ->
                            user.presence = newState.presence

                            when (newState.presence) {
                                is Presence.Online -> ChatManagerEvent.UserCameOnline(user)
                                is Presence.Offline -> ChatManagerEvent.UserWentOffline(user)
                            }
                        }.recover { error ->
                            ChatManagerEvent.ErrorOccurred(error)
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
                    ChatManagerEvent.CurrentUserReceived(createCurrentUser(event))
                is UserSubscriptionEvent.AddedToRoomEvent ->
                    ChatManagerEvent.CurrentUserAddedToRoom(event.room)
                is UserSubscriptionEvent.RemovedFromRoomEvent ->
                    ChatManagerEvent.CurrentUserRemovedFromRoom(event.roomId)
                is UserSubscriptionEvent.RoomUpdatedEvent ->
                    ChatManagerEvent.RoomUpdated(event.room)
                is UserSubscriptionEvent.RoomDeletedEvent ->
                    ChatManagerEvent.RoomDeleted(event.roomId)
                is UserSubscriptionEvent.LeftRoomEvent ->
                            userService.fetchUserBy(event.userId).flatMapResult { user ->
                                roomService.roomStore[event.roomId]
                                        .orElse { Errors.other("room ${event.roomId} not found.") }
                                        .map<ChatManagerEvent> { room -> ChatManagerEvent.UserLeftRoom(user, room) }
                            }.waitAndRecover()
                is UserSubscriptionEvent.JoinedRoomEvent ->
                            userService.fetchUserBy(event.userId).flatMapResult { user ->
                                roomService.roomStore[event.roomId]
                                        .orElse { Errors.other("room ${event.roomId} not found.") }
                                        .map<ChatManagerEvent> { room -> ChatManagerEvent.UserJoinedRoom(user, room) }
                            }.waitAndRecover()
                is UserSubscriptionEvent.ErrorOccurred ->
                    ChatManagerEvent.ErrorOccurred(event.error)
            }

    private fun Future<Result<ChatManagerEvent, Error>>.waitAndRecover() =
        this.waitOr { ChatManagerEvent.ErrorOccurred(Errors.other(it)).asSuccess() }.recover { ChatManagerEvent.ErrorOccurred(it) }

    private fun createCurrentUser(initialState: UserSubscriptionEvent.InitialState) = CurrentUser(
            id = initialState.currentUser.id,
            avatarURL = initialState.currentUser.avatarURL,
            customData = initialState.currentUser.customData,
            name = initialState.currentUser.name,
            chatManager = this,
            client = createPlatformClient(InstanceType.DEFAULT)
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

    /**
     * Tries to close all pending subscriptions and resources
     */
    fun close(): Result<Unit, Error> = try {
        userSubscription.unsubscribe()
        presenceSubscription.unsubscribe()
        cursorSubscription.unsubscribe()
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