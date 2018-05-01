package com.pusher.chatkit

import com.pusher.chatkit.network.typeToken
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Wait
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.waitOr
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.*
import java.lang.reflect.Type
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

sealed class UserSubscriptionEvent

data class InitialState(val rooms: List<Room>, val user: User) : UserSubscriptionEvent()
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
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        typeResolver = ::userSubscriptionType
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
                    val combinedRoomUserIds = rooms.flatMap { it.memberUserIds }.toSet()
                    updateExistingRooms(rooms).forEach(consumeEvent)
                    chatManager.roomStore += rooms
                    currentUser?.apply {
                        presenceSubscription.unsubscribe()
                        updateWithPropertiesOf(user)
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

    private fun createCurrentUser(initialState: InitialState, cursors: Map<Int, Cursor>) = CurrentUser(
        apiInstance = apiInstance,
        createdAt = initialState.user.createdAt,
        cursors = cursors.toMutableMap(),
        cursorsInstance = cursorsInstance,
        id = initialState.user.id,
        logger = logger,
        filesInstance = filesInstance,
        presenceInstance = presenceInstance,
        tokenParams = tokenParams,
        tokenProvider = tokenProvider,
        userStore = userStore,
        avatarURL = initialState.user.avatarURL,
        customData = initialState.user.customData,
        name = initialState.user.name,
        updatedAt = initialState.user.updatedAt,
        chatManager = chatManager
    )

    private fun userSubscriptionType(eventId: String): Result<Type, Error> = when(eventId) {
        "initial_state" -> typeToken<InitialState>().asSuccess()
        "added_to_room" -> typeToken<AddedToRoomEvent>().asSuccess()
        "removed_from_room" -> typeToken<RemovedFromRoomEvent>().asSuccess()
        "room_updated" -> typeToken<RoomUpdatedEvent>().asSuccess()
        "room_deleted" -> typeToken<RoomDeletedEvent>().asSuccess()
        "user_joined" -> typeToken<UserJoinedEvent>().asSuccess()
        "user_left" -> typeToken<UserLeftEvent>().asSuccess()
        else -> Errors.other("Invalid event name: $eventId").asFailure()
    }

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

    private fun handleInitialState(initialState: InitialState): Future<Result<ChatManagerEvent, Error>> =
        getCursors().mapResult { cursors ->
            with(currentUser) {
                when (this) {
                    null -> CurrentUser(
                        apiInstance = apiInstance,
                        createdAt = initialState.user.createdAt,
                        cursors = cursors.toMutableMap(),
                        cursorsInstance = cursorsInstance,
                        id = initialState.user.id,
                        logger = logger,
                        filesInstance = filesInstance,
                        presenceInstance = presenceInstance,
                        tokenParams = tokenParams,
                        tokenProvider = tokenProvider,
                        userStore = userStore,
                        avatarURL = initialState.user.avatarURL,
                        customData = initialState.user.customData,
                        name = initialState.user.name,
                        updatedAt = initialState.user.updatedAt,
                        chatManager = chatManager
                    )
                    else -> this.also {
                        it.presenceSubscription.unsubscribe()
                        it.updateWithPropertiesOf(initialState.user)
                    }
                }
            }
        }.let { user -> handleUpdatedUser(user, initialState) }


//        launch {
//        logger.verbose("Initial state received $initialState")
//        chatManager.roomStore += initialState.rooms
//        getCursors().mapResult { cursors ->
//            with(currentUser) {
//                when (this) {
//                    null -> CurrentUser(
//                        apiInstance = apiInstance,
//                        createdAt = initialState.currentUser.createdAt,
//                        cursors = cursors.toMutableMap(),
//                        cursorsInstance = cursorsInstance,
//                        id = initialState.currentUser.id,
//                        logger = logger,
//                        filesInstance = filesInstance,
//                        presenceInstance = presenceInstance,
//                        tokenParams = tokenParams,
//                        tokenProvider = tokenProvider,
//                        userStore = userStore,
//                        avatarURL = initialState.currentUser.avatarURL,
//                        customData = initialState.currentUser.customData,
//                        name = initialState.currentUser.name,
//                        updatedAt = initialState.currentUser.updatedAt,
//                        chatManager = chatManager
//                    )
//                    else -> this.also {
//                        it.presenceSubscription.unsubscribe()
//                        it.updateWithPropertiesOf(initialState.currentUser)
//                    }
//                }
//            }
//        }.onReady {
//            it.fold({ error ->
//                broadcast(ErrorOccurred(error))
//            }, { user ->
//                handleUpdatedUser(user, initialState)
//            })
//        }
//
//    }

    private fun handleUpdatedUser(user: Future<Result<CurrentUser, Error>>, initialState: InitialState): Future<Result<ChatManagerEvent, Error>> =
        TODO()

//    {
//        val combinedRoomUserIds = initialState.rooms.flatMap { it.memberUserIds }.toSet()
//        chatManager.roomStore += initialState.rooms
//        val promisedEvents = when {
//            combinedRoomUserIds.isNotEmpty() -> fetchDetailsForUsers(combinedRoomUserIds)
//                .mapResult { updateExistingRooms(initialState.rooms) }
//                .mapResult { listOf(CurrentUserReceived(user)) + it }
//            else -> listOf(CurrentUserReceived(user)).asSuccess<List<ChatManagerEvent>, elements.Error>().asPromise()
//        }
//
//            .recover { listOf(ErrorOccurred(it)) }
//            .onReady { events -> eventspromisedEvents.forEach { broadcast(it) } }
//        currentUser = user
//
//        launch {
//            user.presenceSubscription.openSubscription().consumeEach { broadcast(it) }
//        }
//    }

    private fun fetchDetailsForUsers(userIds: Set<String>): Future<Result<List<User>, elements.Error>> =
        chatManager.userService().fetchUsersBy(userIds)

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
