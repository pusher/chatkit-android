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
import elements.Errors
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates

data class InitialState(
    val rooms: List<Room>, //TODO: might need to use a different subsctructure for this
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
    consumeEvent: (ChatKitEvent) -> Unit
) : Subscription {

    private val apiInstance get() = chatManager.apiInstance
    private val cursorsInstance get() = chatManager.cursorsInstance
    private val filesInstance get() = chatManager.filesInstance
    private val presenceInstance get() = chatManager.presenceInstance

    private var headers: Headers by Delegates.observable(mutableMapOf()) { _, _, _ ->
        logger.verbose("OnOpen $headers")
    }
    private val subscription: Subscription

    private val broadcast = { event: ChatKitEvent ->
        launch { consumeEvent(event) }
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

    override fun unsubscribe() =
        subscription.unsubscribe()

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
                    else -> throw Error("Invalid event name: $eventName")
                }
            }
    }

    private fun handleUserLeft(userId: String, roomId: Int) {
        userStore.findOrGetUser(id = userId)
            .fold({ error ->
                broadcastError(error, "User left a room but I failed getting it: $userId")
            }, { user ->
                val room = currentUser?.getRoom(roomId)
                room!!.removeUser(userId)
                broadcast(UserLeftRoom(user, room))
            })
    }

    private fun handleUserJoined(userId: String, roomId: Int) {
        userStore.findOrGetUser(id = userId)
            .fold({ error ->
                broadcastError(error, "User joined a room but I failed getting it: $userId")
            }, { user ->
                val room = currentUser?.getRoom(roomId)
                room!!.userStore.addOrMerge(user)
                broadcast(UserJoinedRoom(user, room))
            })
    }

    private fun handleRoomDeleted(roomId: Int) {
        currentUser?.roomStore?.roomsMap?.remove(roomId)
        broadcast(RoomDeleted(roomId))
    }

    private fun handleRoomUpdated(room: Room) {
        currentUser?.roomStore?.addOrMerge(room)
        broadcast(RoomUpdated(room))
    }

    private fun handleRemovedFromRoom(roomId: Int) {
        currentUser?.roomStore?.roomsMap?.remove(roomId)
        broadcast(CurrentUserRemovedFromRoom(roomId))
    }

    private fun handleAddedToRoom(room: Room) {
        currentUser?.roomStore?.addOrMerge(room)
        broadcast(CurrentUserAddedToRoom(room))
    }

    private var currentUser: CurrentUser? = null

    private fun handleInitialState(initialState: InitialState) = launch {
        logger.verbose("Initial state received $initialState")
        getCursors().mapResult { cursors ->
            with(currentUser) {
                when (this) {
                    null -> CurrentUser(
                        apiInstance = apiInstance,
                        avatarURL = initialState.currentUser.avatarURL,
                        createdAt = initialState.currentUser.createdAt,
                        cursors = cursors.toMutableMap(),
                        cursorsInstance = cursorsInstance,
                        customData = initialState.currentUser.customData,
                        id = initialState.currentUser.id,
                        logger = logger,
                        name = initialState.currentUser.name,
                        rooms = initialState.rooms,
                        filesInstance = filesInstance,
                        presenceInstance = presenceInstance,
                        tokenProvider = tokenProvider,
                        tokenParams = tokenParams,
                        updatedAt = initialState.currentUser.updatedAt,
                        userStore = userStore
                    )
                    else -> this.also {
                        it.presenceSubscription.unsubscribe()
                        it.updateWithPropertiesOf(initialState.currentUser)
                    }
                }
            }
        }.mapResult { user ->
            val combinedRoomUserIds = initialState.rooms.flatMap { it.memberUserIds }.toSet()
            user.roomStore += initialState.rooms
            val promisedEvents = when {
                combinedRoomUserIds.isNotEmpty() -> user.fetchDetailsForUsers(combinedRoomUserIds)
                    .mapResult { user.updateExistingRooms(initialState.rooms) }
                    .mapResult { listOf(CurrentUserReceived(user)) + it }
                else -> listOf(CurrentUserReceived(user)).asSuccess<List<ChatKitEvent>, elements.Error>().asPromise()
            }
            promisedEvents
                .recover { listOf(ErrorOccurred(it)) }
                .onReady { events ->
                    events.forEach { broadcast(it) }
                }
            launch {
                user.presenceSubscription.openSubscription().consumeEach { broadcast(it) }
            }
            currentUser = user
        }.recover {error ->
            broadcast(ErrorOccurred(error))
            logger.error("handleInitialState", Error("Error: $error"))
        }
    }

    private fun broadcastError(error: elements.Error, message: String) {
        logger.error(message)
        broadcast(ErrorOccurred(error))
    }

    private fun CurrentUser.fetchDetailsForUsers(userIds: Set<String>): Promise<Result<List<User>, elements.Error>> =
        userStore.fetchUsersWithIds(userIds)
            .mapResult { users ->
                rooms().forEach { room ->
                    room.userStore += room.memberUserIds
                        .mapNotNull { id -> users.find { it.id == id } }
                }
                users
            }

    private fun CurrentUser.updateExistingRooms(roomsForConnection: List<Room>): List<ChatKitEvent> =
        (rooms() - roomsForConnection)
            .map { CurrentUserRemovedFromRoom(it.id) }


}
