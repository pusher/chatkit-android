package com.pusher.chatkit

import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.users.UserSubscriptionEventParser
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Wait
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.waitOr
import com.pusher.platform.tokenProvider.TokenProvider
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

class UserSubscription(
    val userId: String,
    private val chatManager: ChatManager,
    path: String,
    val userStore: GlobalUserStore,
    val tokenProvider: TokenProvider,
    val tokenParams: ChatkitTokenParams?,
    val logger: Logger,
    private val consumeEvent: (ChatManagerEvent) -> Unit
) : Subscription {

    private val apiInstance get() = chatManager.apiInstance
    private val cursorsInstance get() = chatManager.cursorsInstance
    private val filesInstance get() = chatManager.filesInstance
    private val presenceInstance get() = chatManager.presenceInstance

    private var headers: Headers = emptyHeaders()

    private val subscription = apiInstance.subscribeResuming(
        path = path,
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
        bodyParser = UserSubscriptionEventParser,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    )

    override fun unsubscribe() {
        subscription.unsubscribe()
        currentUser?.roomSubscriptions?.forEach(RoomSubscription::unsubscribe)
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
        initialState: InitialState,
        cursors: Map<Int, Cursor>
    ) = CurrentUser(
        apiInstance = apiInstance,
        createdAt = initialState.currentUser.createdAt,
        cursors = cursors.toMutableMap(),
        cursorsInstance = cursorsInstance,
        id = initialState.currentUser.id,
        logger = logger,
        filesInstance = filesInstance,
        presenceInstance = presenceInstance,
        tokenParams = tokenParams,
        tokenProvider = tokenProvider,
        userStore = userStore,
        avatarURL = initialState.currentUser.avatarURL,
        customData = initialState.currentUser.customData,
        name = initialState.currentUser.name,
        updatedAt = initialState.currentUser.updatedAt,
        chatManager = chatManager
    )

    private fun UserSubscriptionEvent.toChatManagerEvent(): Future<Result<ChatManagerEvent, Error>> = when (this) {
        is InitialState -> getCursors().mapResult { cursors ->
            CurrentUserReceived(createCurrentUser(this, cursors))
        }
        is AddedToRoomEvent -> CurrentUserAddedToRoom(room).toFutureSuccess()
        is RoomUpdatedEvent -> RoomUpdated(room).toFutureSuccess()
        is RoomDeletedEvent -> RoomDeleted(roomId).toFutureSuccess()
        is RemovedFromRoomEvent -> CurrentUserRemovedFromRoom(roomId).toFutureSuccess()
        is UserLeftEvent -> chatManager.userService().fetchUserBy(userId).flatMapResult { user ->
            chatManager.roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserLeftRoom(user, room) }
        }
        is UserJoinedEvent -> chatManager.userService().fetchUserBy(userId).flatMapResult { user ->
            chatManager.roomStore[roomId]
                .orElse { Errors.other("room $roomId not found.") }
                .map<ChatManagerEvent> { room -> UserJoinedRoom(user, room) }
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
            tokenParams = tokenParams
        )

}
