package com.pusher.chatkit.users

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.CurrentUser
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.*
import java.lang.Thread.sleep
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit.SECONDS
import com.pusher.chatkit.users.UserSubscriptionEvent.*
import com.pusher.platform.logger.Logger

private const val USERS_PATH = "users"

internal class UserSubscription(
    private val userId: String,
    private val client: PlatformClient,
    private val roomService: RoomService,
    private val userService: UserService,
    private val cursorService: CursorService,
    private val presenceService: PresenceService,
    private val consumeEvent: (ChatManagerEvent) -> Unit,
    private val logger: Logger
) : ChatkitSubscription {
    private var headers: Headers = emptyHeaders()

    private lateinit var userSubscription: Subscription
    private lateinit var presenceSubscription: Subscription
    private lateinit var cursorSubscription: Subscription

    override fun connect(): ChatkitSubscription {
        val deferredUserSubscription = Futures.schedule {
            ResolvableSubscription(
                client = client,
                path = USERS_PATH,
                listeners = SubscriptionListeners(
                    onOpen = { headers ->
                        logger.verbose("[User] OnOpen $headers")
                        this@UserSubscription.headers = headers
                    },
                    onEvent = { event: SubscriptionEvent<UserSubscriptionEvent> ->
                        event.body
                            .applySideEffects()
                            .toChatManagerEvent()
                            .waitOr(Wait.For(10, SECONDS)) { ErrorOccurred(Errors.other(it)).asSuccess() }
                            .recover { ErrorOccurred(it) }
                            .also { if (it is CurrentUserReceived) currentUser = it.currentUser }
                            .also(consumeEvent)
                            .also { logger.verbose("[User] Event received $it") }
                    },
                    onError = { error -> consumeEvent(ErrorOccurred(error)) },
                    onSubscribe = { logger.verbose("[User] Subscription established.") },
                    onRetrying = { logger.verbose("[User] Subscription lost. Trying again.") },
                    onEnd = { error -> logger.verbose("[User] Subscription ended with: $error") }
                ),
                messageParser = UserSubscriptionEventParser,
                resolveOnFirstEvent = true
            ).connect()
        }

        val deferredPresenceSubscription = Futures.schedule {
            presenceService.subscribe(userId, consumeEvent)
        }

        val deferredCursorSubscription = Futures.schedule {
            cursorService.subscribeForUser(userId) { event ->
                when(event) {
                    is CursorSubscriptionEvent.OnCursorSet ->
                        consumeEvent(ChatManagerEvent.NewReadCursor(event.cursor))
                }
            }
        }

        cursorSubscription = deferredCursorSubscription.wait()
        userSubscription = deferredUserSubscription.wait()
        presenceSubscription = deferredPresenceSubscription.wait()

        return this
    }

    override fun unsubscribe() {
        userSubscription.unsubscribe()
        cursorSubscription.unsubscribe()
        presenceSubscription.unsubscribe()
        currentUser?.close()
    }

    private fun getCursors(): Future<Result<Map<Int, Cursor>, Error>> =
        cursorService.request(userId)
            .mapResult { cursors -> cursors.map { it.roomId to it }.toMap() }

    private fun UserSubscriptionEvent.applySideEffects(): UserSubscriptionEvent = this.apply {
        when (this) {
            is InitialState -> {
                for (event in updateExistingRooms(rooms)) {
                    consumeEvent(event)
                }
                roomService.roomStore += rooms
                this@UserSubscription.currentUser?.apply {
                    close()
                    updateWithPropertiesOf(currentUser)
                }
            }
            is AddedToRoomEvent -> roomService.roomStore += room
            is RoomUpdatedEvent -> roomService.roomStore += room
            is RoomDeletedEvent -> roomService.roomStore -= roomId
            is RemovedFromRoomEvent -> roomService.roomStore -= roomId
            is LeftRoomEvent -> roomService.roomStore[roomId]?.removeUser(userId)
            is JoinedRoomEvent -> roomService.roomStore[roomId]?.addUser(userId)
            is StartedTyping -> typingTimers
                .firstOrNull { it.userId == userId && it.roomId == roomId }
                ?: TypingTimer(userId, roomId).also { typingTimers += it }
                    .triggerTyping()
        }
    }

    private val typingTimers = mutableListOf<TypingTimer>()

    inner class TypingTimer(val userId: String, val roomId: Int) {

        private var job: Future<Unit>? = null

        fun triggerTyping() {
            if (job.isActive) job?.cancel()
            job = scheduleStopTyping()
        }

        private fun scheduleStopTyping(): Future<Unit> = Futures.schedule {
            sleep(1_500)
            val event = UserSubscriptionEvent.StoppedTyping(userId, roomId)
                .toChatManagerEvent()
                .wait()
                .recover { ErrorOccurred(it) }
            consumeEvent(event)
        }

        private val <A> Future<A>?.isActive
            get() = this != null && !isDone && !isCancelled

    }

    private fun createCurrentUser(initialState: InitialState) = CurrentUser(
        id = initialState.currentUser.id,
        avatarURL = initialState.currentUser.avatarURL,
        customData = initialState.currentUser.customData,
        name = initialState.currentUser.name,
        chatManager = chatManager
    )

    private fun UserSubscriptionEvent.toChatManagerEvent(): Future<Result<ChatManagerEvent, Error>> = when (this) {
        is InitialState -> getCursors().mapResult { cursors ->
            cursorService.saveCursors(cursors)
            CurrentUserReceived(createCurrentUser(this))
        }
        is AddedToRoomEvent -> CurrentUserAddedToRoom(room).toFutureSuccess()
        is RoomUpdatedEvent -> RoomUpdated(room).toFutureSuccess()
        is RoomDeletedEvent -> RoomDeleted(roomId).toFutureSuccess()
        is RemovedFromRoomEvent -> CurrentUserRemovedFromRoom(roomId).toFutureSuccess()
        is LeftRoomEvent -> userService.fetchUserBy(userId).flatMapResult { user ->
            roomService.roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserLeftRoom(user, room) }
        }
        is JoinedRoomEvent -> userService.fetchUserBy(userId).flatMapResult { user ->
            roomService.roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserJoinedRoom(user, room) }
        }
        is StartedTyping -> userService.fetchUserBy(userId).flatMapFutureResult { user ->
            roomService.fetchRoomBy(user.id, roomId).mapResult { room ->
                ChatManagerEvent.UserStartedTyping(user, room) as ChatManagerEvent
            }
        }
        is StoppedTyping -> userService.fetchUserBy(userId).flatMapFutureResult { user ->
            roomService.fetchRoomBy(user.id, roomId).mapResult { room ->
                ChatManagerEvent.UserStoppedTyping(user, room) as ChatManagerEvent
            }
        }
    }

    private fun ChatManagerEvent.toFutureSuccess() =
        asSuccess<ChatManagerEvent, Error>().toFuture()

    private var currentUser: CurrentUser? = null

    private fun updateExistingRooms(roomsForConnection: List<Room>): List<ChatManagerEvent> {
        return (roomService.roomStore.toList() - roomsForConnection)
            .map { CurrentUserRemovedFromRoom(it.id) }
    }
}