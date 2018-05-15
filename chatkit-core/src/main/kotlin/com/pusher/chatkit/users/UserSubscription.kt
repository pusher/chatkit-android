package com.pusher.chatkit.users

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.CurrentUser
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.rooms.Room
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.*
import java.lang.Thread.sleep
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit.SECONDS

internal sealed class UserSubscriptionEvent

internal data class InitialState(val rooms: List<Room>, val currentUser: User) : UserSubscriptionEvent()
internal data class AddedToRoomEvent(val room: Room) : UserSubscriptionEvent()
internal data class RoomUpdatedEvent(val room: Room) : UserSubscriptionEvent()
internal data class RoomDeletedEvent(val roomId: Int) : UserSubscriptionEvent()
internal data class RemovedFromRoomEvent(val roomId: Int) : UserSubscriptionEvent()
internal data class UserLeftEvent(val roomId: Int, val userId: String) : UserSubscriptionEvent()
internal data class UserJoinedEvent(val roomId: Int, val userId: String) : UserSubscriptionEvent()
internal data class UserStartedTyping(val userId: String, val roomId: Int) : UserSubscriptionEvent()
internal data class UserStoppedTyping(val userId: String, val roomId: Int) : UserSubscriptionEvent()

private const val USERS_PATH = "users"

class UserSubscription(
    val userId: String,
    private val chatManager: ChatManager,
    private val consumeEvent: (ChatManagerEvent) -> Unit
) : Subscription {

    private val apiInstance get() = chatManager.apiInstance

    private val tokenProvider = chatManager.tokenProvider
    private val logger = chatManager.dependencies.logger
    private val roomStore = chatManager.roomService.roomStore

    private var headers: Headers = emptyHeaders()

    private val subscription = apiInstance.subscribeResuming(
        path = USERS_PATH,
        listeners = SubscriptionListeners(
            onOpen = { headers ->
                logger.verbose("OnOpen $headers")
                this.headers = headers
            },
            onEvent = { event: SubscriptionEvent<UserSubscriptionEvent> ->
                event.body
                    .applySideEffects()
                    .toChatManagerEvent()
                    .waitOr(Wait.For(10, SECONDS), { ErrorOccurred(Errors.other(it)).asSuccess() })
                    .recover { ErrorOccurred(it) }
                    .also { if (it is CurrentUserReceived) currentUser = it.currentUser }
                    .also(consumeEvent)
                    .also { logger.verbose("Event received $it") }
            },
            onError = { error -> consumeEvent(ErrorOccurred(error)) },
            onSubscribe = { logger.verbose("Subscription established.") },
            onRetrying = { logger.verbose("Subscription lost. Trying again.") },
            onEnd = { error -> logger.verbose("Subscription ended with: $error") }
        ),
        messageParser = UserSubscriptionEventParser,
        tokenProvider = this.tokenProvider
    )

    private val presenceSubscription = chatManager.presenceService.subscribeToPresence(userId, consumeEvent)

    private val cursorSubscription = chatManager.cursorService.subscribeForUser(userId) { event ->
        when(event) {
            is CursorSubscriptionEvent.OnCursorSet -> consumeEvent(NewReadCursor(event.cursor))
        }
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
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
                updateExistingRooms(rooms).forEach(consumeEvent)
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
            is UserLeftEvent -> roomStore[roomId]?.removeUser(userId)
            is UserJoinedEvent -> roomStore[roomId]?.addUser(userId)
            is UserStartedTyping -> typingTimers
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
            val event = UserStoppedTyping(userId, roomId)
                .toChatManagerEvent()
                .wait()
                .recover { ErrorOccurred(it) }
            consumeEvent(event)
        }

        private val <A> Future<A>?.isActive
            get() = this != null && !isDone && !isCancelled

    }

    private fun createCurrentUser(
        initialState: InitialState
    ) = CurrentUser(
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
        is UserLeftEvent -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserLeftRoom(user, room) }
        }
        is UserJoinedEvent -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserJoinedRoom(user, room) }
        }
        is UserStartedTyping -> chatManager.userService.fetchUserBy(userId).flatMapFutureResult { user ->
            chatManager.roomService.fetchRoomBy(user.id, roomId).mapResult { room ->
                ChatManagerEvent.UserStartedTyping(user, room) as ChatManagerEvent
            }
        }
        is UserStoppedTyping -> chatManager.userService.fetchUserBy(userId).flatMapFutureResult { user ->
            chatManager.roomService.fetchRoomBy(user.id, roomId).mapResult { room ->
                ChatManagerEvent.UserStoppedTyping(user, room) as ChatManagerEvent
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
