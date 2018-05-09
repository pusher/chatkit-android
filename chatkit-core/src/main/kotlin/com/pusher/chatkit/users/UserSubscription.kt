package com.pusher.chatkit.users

import com.pusher.chatkit.*
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.rooms.Room
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.Wait
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.waitOr
import com.pusher.util.*
import elements.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit.*

sealed class UserSubscriptionEvent

data class InitialState(val rooms: List<Room>, val currentUser: User) : UserSubscriptionEvent()
data class AddedToRoomEvent(val room: Room) : UserSubscriptionEvent()
data class RoomUpdatedEvent(val room: Room) : UserSubscriptionEvent()
data class RoomDeletedEvent(val roomId: Int) : UserSubscriptionEvent()
data class RemovedFromRoomEvent(val roomId: Int) : UserSubscriptionEvent()
data class UserLeftEvent(val roomId: Int, val userId: String) : UserSubscriptionEvent()
data class UserJoinedEvent(val roomId: Int, val userId: String) : UserSubscriptionEvent()
data class UserStartedTyping(val userId: String, val roomId: Int) : UserSubscriptionEvent()

private const val USERS_PATH = "users"

class UserSubscription(
    val userId: String,
    private val chatManager: ChatManager,
    private val consumeEvent: (ChatManagerEvent) -> Unit
) : Subscription {

    private val apiInstance get() = chatManager.apiInstance
    private val cursorsInstance get() = chatManager.cursorsInstance
    private val filesInstance get() = chatManager.filesInstance

    private val tokenProvider = chatManager.tokenProvider
    private val tokenParams = chatManager.dependencies.tokenParams
    private val logger = chatManager.dependencies.logger

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
        tokenProvider = this.tokenProvider,
        tokenParams = this.tokenParams
    )

    private val presenceSubscription = chatManager.presenceService.subscribeToPresence(userId, consumeEvent)

    private val cursorSubscription = chatManager.cursorService.subscribeToCursors(userId) { event ->
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
        cursorsRequest.mapResult { cursors -> cursors.map { it.roomId to it }.toMap() }

    private fun UserSubscriptionEvent.applySideEffects(): UserSubscriptionEvent = this.apply {
            when (this) {
                is InitialState -> {
                    updateExistingRooms(rooms).forEach(consumeEvent)
                    chatManager.roomStore += rooms
                    this@UserSubscription.currentUser?.apply {
                        close()
                        updateWithPropertiesOf(currentUser)
                    }
                }
                is AddedToRoomEvent -> chatManager.roomStore += room
                is RoomUpdatedEvent -> chatManager.roomStore += room
                is RoomDeletedEvent -> chatManager.roomStore -= roomId
                is RemovedFromRoomEvent -> chatManager.roomStore -= roomId
                is UserLeftEvent -> chatManager.roomStore[roomId]?.removeUser(userId)
                is UserJoinedEvent -> chatManager.roomStore[roomId]?.addUser(userId)
            }
    }

    private fun createCurrentUser(
        initialState: InitialState
    ) = CurrentUser(
        id = initialState.currentUser.id,
        filesInstance = filesInstance,
        tokenParams = tokenParams,
        tokenProvider = tokenProvider,
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
            chatManager.roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserLeftRoom(user, room) }
        }
        is UserJoinedEvent -> chatManager.userService.fetchUserBy(userId).flatMapResult { user ->
            chatManager.roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserJoinedRoom(user, room) }
        }
        is UserStartedTyping -> chatManager.userService.fetchUserBy(userId).mapResult { user ->
            ChatManagerEvent.UserStartedTyping(user) as ChatManagerEvent
        }
    }

    private fun ChatManagerEvent.toFutureSuccess() =
        asSuccess<ChatManagerEvent, Error>().toFuture()

    private var currentUser: CurrentUser? = null

    private fun updateExistingRooms(roomsForConnection: List<Room>): List<ChatManagerEvent> =
        (chatManager.roomStore.toList() - roomsForConnection)
            .map { CurrentUserRemovedFromRoom(it.id) }


    private val cursorsRequest: Future<Result<List<Cursor>, Error>>
        get() = cursorsInstance.request(
            options = RequestOptions(
                method = "GET",
                path = "/cursors/0/users/$userId"
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            responseParser = { it.parseAs<List<Cursor>>() }
        )

}
