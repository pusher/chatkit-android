package com.pusher.chatkit

import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.*
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates

data class InitialState(
    val rooms: List<Room>, //TODO: might need to use a different substructure for this
    val currentUser: User
)

//Added to room
data class RoomEvent(
    val room: Room
)

//Room deleted, removed from room
data class RoomIdEvent(
    val roomId: Int
)

//User joined or left
data class UserChangeEvent(
    val roomId: Int,
    val userId: String
)

class UserSubscription(
    val userId: String,
    private val chatManager: ChatManager,
    path: String,
    val userStore: GlobalUserStore,
    val tokenProvider: TokenProvider,
    val tokenParams: ChatkitTokenParams?,
    val logger: Logger,
    consumeEvent: (ChatManagerEvent) -> Unit
) : Subscription {

    private val apiInstance get() = chatManager.apiInstance
    private val cursorsInstance get() = chatManager.cursorsInstance
    private val filesInstance get() = chatManager.filesInstance
    private val presenceInstance get() = chatManager.presenceInstance

    private var headers: Headers by Delegates.observable(mutableMapOf()) { _, _, _ ->
        logger.verbose("OnOpen $headers")
    }
    private val subscription: Subscription

    private val broadcast = { event: ChatManagerEvent ->
        launch {
            consumeEvent(event)
        }
    }

    init {
        subscription = apiInstance.subscribeResuming(
            path = path,
            listeners = SubscriptionListeners(
                onOpen = { headers -> this.headers = headers },
                onEvent = { event -> handleEvent(event) },
                onError = { error -> broadcastError(error, "Error $error") },
                onSubscribe = { logger.verbose("Subscription established.") },
                onRetrying = { logger.verbose("Subscription lost. Trying again.") },
                onEnd = { error -> logger.verbose("Subscription ended with: $error") }
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams
        )
    }

    override fun unsubscribe() {
        subscription.unsubscribe()
        currentUser?.roomSubscriptions?.forEach(RoomSubscription::unsubscribe)
    }

    private fun getCursors(): Promise<Result<Map<Int, Cursor>, elements.Error>> = cursorsInstance.request(
        options = RequestOptions(
            method = "GET",
            path = "/cursors/0/users/$userId"
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).parseResponseWhenReady<Array<Cursor>>()
        .mapResult { cursors -> cursors.map { it.roomId to it }.toMap() }

    private fun handleEvent(event: SubscriptionEvent) {
        event.body.parseAs<ChatEvent>()
            .map { (eventName, _, _, data) ->
                when (eventName) {
                    "initial_state" -> data.parseAs<InitialState>().map { handleInitialState(it) }
                    "added_to_room" -> data.parseAs<RoomEvent>().map { handleAddedToRoom(it.room) }
                    "removed_from_room" -> data.parseAs<RoomIdEvent>().map { handleRemovedFromRoom(it.roomId) }
                    "room_updated" -> data.parseAs<RoomEvent>().map { handleRoomUpdated(it.room) }
                    "room_deleted" -> data.parseAs<RoomIdEvent>().map { handleRoomDeleted(it.roomId) }
                    "user_joined" -> data.parseAs<UserChangeEvent>().map { handleUserJoined(userId, it.roomId) }
                    "user_left" -> data.parseAs<UserChangeEvent>().map { handleUserLeft(userId, it.roomId) }
                    else -> handleError(Errors.other("Invalid event name: $eventName"))
                }
            }
    }

    private fun handleError(error: elements.Error) {
        broadcast(ErrorOccurred(error))
    }

    private fun handleUserLeft(userId: String, roomId: Int) {
        chatManager.userService().fetchUserBy(userId)
            .fold({ error ->
                broadcastError(error, "User($userId) left a room couldn't recover it ($roomId) ")
            }, { user ->
                val room = chatManager.roomStore[roomId]
                room!!.removeUser(userId)
                broadcast(UserLeftRoom(user, room))
            })
    }

    private fun handleUserJoined(userId: String, roomId: Int) {
        chatManager.userService().fetchUserBy(userId)
            .fold({ error ->
                ChatManagerEvent.onError(error)
            }, { user ->
                val room = chatManager.roomStore[roomId]
                when (room) {
                    null -> ChatManagerEvent.onError(OtherError("Could not find room with id: $roomId"))
                    else -> ChatManagerEvent.onUserJoinedRoom(user, room)
                }
            })
            .onReady { event ->
                if (event is UserJoinedRoom) {
                    event.room.memberUserIds() += userId
                }
                broadcast(event)
            }
    }

    private fun handleRoomDeleted(roomId: Int) {
        chatManager.roomStore -= roomId
        broadcast(RoomDeleted(roomId))
    }

    private fun handleRoomUpdated(room: Room) {
        chatManager.roomStore += room
        broadcast(RoomUpdated(room))
    }

    private fun handleRemovedFromRoom(roomId: Int) {
        chatManager.roomStore -= roomId
        broadcast(CurrentUserRemovedFromRoom(roomId))
    }

    private fun handleAddedToRoom(room: Room) {
        chatManager.roomStore += room
        broadcast(CurrentUserAddedToRoom(room))
    }

    private var currentUser: CurrentUser? = null

    private fun handleInitialState(initialState: InitialState) = launch {
        logger.verbose("Initial state received $initialState")
        chatManager.roomStore += initialState.rooms
        getCursors().mapResult { cursors ->
            with(currentUser) {
                when (this) {
                    null -> CurrentUser(
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
                    else -> this.also {
                        it.presenceSubscription.unsubscribe()
                        it.updateWithPropertiesOf(initialState.currentUser)
                    }
                }
            }
        }.onReady {
            it.fold({ error ->
                broadcast(ErrorOccurred(error))
            }, { user ->
                handleUpdatedUser(user, initialState)
            })
        }

    }

    private fun handleUpdatedUser(user: CurrentUser, initialState: InitialState) {
        val combinedRoomUserIds = initialState.rooms.flatMap { it.memberUserIds }.toSet()
        chatManager.roomStore += initialState.rooms
        val promisedEvents = when {
            combinedRoomUserIds.isNotEmpty() -> fetchDetailsForUsers(combinedRoomUserIds)
                .mapResult { updateExistingRooms(initialState.rooms) }
                .mapResult { listOf(CurrentUserReceived(user)) + it }
            else -> listOf(CurrentUserReceived(user)).asSuccess<List<ChatManagerEvent>, elements.Error>().asPromise()
        }
        promisedEvents
            .recover { listOf(ErrorOccurred(it)) }
            .onReady { events -> events.forEach { broadcast(it) } }
        currentUser = user

        launch {
            user.presenceSubscription.openSubscription().consumeEach { broadcast(it) }
        }
    }

    private fun broadcastError(error: elements.Error, message: String = error.reason) {
        logger.error(message)
        broadcast(ErrorOccurred(error))
    }

    private fun fetchDetailsForUsers(userIds: Set<String>): Promise<Result<List<User>, elements.Error>> =
        chatManager.userService().fetchUsersBy(userIds)

    private fun updateExistingRooms(roomsForConnection: List<Room>): List<ChatManagerEvent> =
        (chatManager.roomStore.toList() - roomsForConnection)
            .map { CurrentUserRemovedFromRoom(it.id) }


}
