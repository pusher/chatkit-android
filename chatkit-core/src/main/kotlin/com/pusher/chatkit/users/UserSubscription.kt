package com.pusher.chatkit.users

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.CurrentUser
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.cursors.CursorSubscription
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.presence.PresenceSubscription
import com.pusher.chatkit.rooms.Room
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
import kotlinx.coroutines.experimental.async

private const val USERS_PATH = "users"

internal class UserSubscription(
    val userId: String,
    private val chatManager: ChatManager,
    private val consumeEvent: (ChatManagerEvent) -> Unit
) : ChatkitSubscription {

    private val logger = chatManager.dependencies.logger
    private val roomStore = chatManager.roomService.roomStore
    private var headers: Headers = emptyHeaders()

    private lateinit var userSubscription: Subscription
    private lateinit var presenceSubscription: Subscription
    private lateinit var cursorSubscription: Subscription

    override suspend fun connect(): ChatkitSubscription {
        val deferredUserSubscription = async {
            ResolvableSubscription(
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
                chatManager = chatManager,
                messageParser = UserSubscriptionEventParser,
                resolveOnFirstEvent = true
            ).connect()
        }

        val deferredPresenceSubscription = async {
            chatManager.presenceService.subscribe(userId, consumeEvent)
        }

        val deferredCursorSubscription = async {
            chatManager.cursorService.subscribeForUser(userId) { event ->
                when(event) {
                    is CursorSubscriptionEvent.OnCursorSet ->
                        consumeEvent(ChatManagerEvent.NewReadCursor(event.cursor))
                }
            }
        }

        cursorSubscription = deferredCursorSubscription.await()
        userSubscription = deferredUserSubscription.await()
        presenceSubscription = deferredPresenceSubscription.await()

        return this
    }

    override fun unsubscribe() {
        userSubscription.unsubscribe()
        cursorSubscription.unsubscribe()
        presenceSubscription.unsubscribe()
        currentUser?.close()
    }

    private fun getCursors(): Future<Result<Map<Int, Cursor>, Error>> =
        chatManager.cursorService.request(userId)
            .mapResult { cursors -> cursors.map { it.roomId to it }.toMap() }

    private fun UserSubscriptionEvent.applySideEffects(): UserSubscriptionEvent = this.apply {
        when (this) {
            is InitialState -> {
                for (event in updateExistingRooms(rooms)) {
                    consumeEvent(event)
                }
                roomStore += rooms
                this@UserSubscription.currentUser?.apply {
                    close()
                    updateWithPropertiesOf(currentUser)
                }
            }
            is AddedToRoomEvent -> roomStore += room
            is RoomUpdatedEvent -> roomStore += room
            is RoomDeletedEvent -> roomStore -= roomId
            is RemovedFromRoomEvent -> roomStore -= roomId
            is LeftRoomEvent -> roomStore[roomId]?.removeUser(userId)
            is JoinedRoomEvent -> roomStore[roomId]?.addUser(userId)
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
            chatManager.cursorService.saveCursors(cursors)
            CurrentUserReceived(createCurrentUser(this))
        }
        is AddedToRoomEvent -> CurrentUserAddedToRoom(room).toFutureSuccess()
        is RoomUpdatedEvent -> RoomUpdated(room).toFutureSuccess()
        is RoomDeletedEvent -> RoomDeleted(roomId).toFutureSuccess()
        is RemovedFromRoomEvent -> CurrentUserRemovedFromRoom(roomId).toFutureSuccess()
        is LeftRoomEvent -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserLeftRoom(user, room) }
        }
        is JoinedRoomEvent -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserJoinedRoom(user, room) }
        }
        is StartedTyping -> chatManager.userService.fetchUserBy(userId).flatMapFutureResult { user ->
            chatManager.roomService.fetchRoomBy(user.id, roomId).mapResult { room ->
                ChatManagerEvent.UserIsTyping(user, room) as ChatManagerEvent
            }
        }
        is StoppedTyping -> chatManager.userService.fetchUserBy(userId).flatMapFutureResult { user ->
            chatManager.roomService.fetchRoomBy(user.id, roomId).mapResult { room ->
                ChatManagerEvent.UserIsTyping(user, room) as ChatManagerEvent
            }
        }
    }

    private fun ChatManagerEvent.toFutureSuccess() =
        asSuccess<ChatManagerEvent, Error>().toFuture()

    private var currentUser: CurrentUser? = null

    private fun updateExistingRooms(roomsForConnection: List<Room>): List<ChatManagerEvent> {
        return (roomStore.toList() - roomsForConnection)
            .map { CurrentUserRemovedFromRoom(it.id) }
    }

}
