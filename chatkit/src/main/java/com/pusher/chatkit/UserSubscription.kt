package com.pusher.chatkit

import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.suspendCoroutine
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

@UsesCoroutines
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

    private val cursors: Deferred<ConcurrentHashMap<Int, Cursor>> = async { getCursors() }
    private var headers: Headers by Delegates.observable(mutableMapOf()) { _, _, _ ->
        logger.verbose("OnOpen $headers")
    }
    private val subscription: Subscription
    private val broadcast = { event: ChatManagerEvent ->
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

    private suspend fun getCursors(): ConcurrentHashMap<Int, Cursor> = suspendCoroutine { cont ->
        val cursorsByRoom: ConcurrentHashMap<Int, Cursor> = ConcurrentHashMap()
        cursorsInstance.request(
            options = RequestOptions(
                method = "GET",
                path = "/cursors/0/users/$userId"
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            onSuccess = { res ->
                val cursors: Array<Cursor> = ChatManager.GSON.fromJson<Array<Cursor>>(
                    res.body()!!.charStream(),
                    Array<Cursor>::class.java
                )
                for (cursor in cursors) {
                    cursorsByRoom[cursor.roomId] = cursor
                }
                cont.resume(cursorsByRoom)
            },
            onFailure = { error ->
                logger.warn("Failed to get cursors: $error")
                broadcast(ErrorOccurred(error))
                cont.resume(cursorsByRoom)
            }
        )
    }

    private fun handleEvent(event: SubscriptionEvent) {

        logger.verbose("Handle Event $event")

        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)
        when (chatEvent.eventName) {
            "initial_state" -> {
                val body = ChatManager.GSON.fromJson<InitialState>(chatEvent.data, InitialState::class.java)
                handleInitialState(body)
            }
            "added_to_room" -> {
                val body = ChatManager.GSON.fromJson<RoomEvent>(chatEvent.data, RoomEvent::class.java)
                handleAddedToRoom(body.room)
            }
            "removed_from_room" -> {
                val body = ChatManager.GSON.fromJson<RoomIdEvent>(chatEvent.data, RoomIdEvent::class.java)
                handleRemovedFromRoom(body.roomId)
            }
            "room_updated" -> {
                val body = ChatManager.GSON.fromJson<RoomEvent>(chatEvent.data, RoomEvent::class.java)
                handleRoomUpdated(body.room)
            }
            "room_deleted" -> {
                val body = ChatManager.GSON.fromJson<RoomIdEvent>(chatEvent.data, RoomIdEvent::class.java)
                handleRoomDeleted(body.roomId)
            }
            "user_joined" -> {
                val body = ChatManager.GSON.fromJson<UserChangeEvent>(chatEvent.data, UserChangeEvent::class.java)
                handleUserJoined(body.userId, body.roomId)

            }
            "user_left" -> {
                val body = ChatManager.GSON.fromJson<UserChangeEvent>(chatEvent.data, UserChangeEvent::class.java)
                handleUserLeft(body.userId, body.roomId)
            }
            else -> {
                throw Error("Invalid event name: ${chatEvent.eventName}")
            }
        }
    }

    private fun handleUserLeft(userId: String, roomId: Int) {
        userStore.findOrGetUser(
            id = userId,
            userListener = UserListener { user ->
                val room = currentUser?.getRoom(roomId)
                room!!.removeUser(userId)
                broadcast(UserLeftRoom(user, room))
            },
            errorListener = ErrorListener { error ->
                broadcastError(error, "User left a room but I failed getting it: $userId")
            })
    }

    private fun handleUserJoined(userId: String, roomId: Int) {

        userStore.findOrGetUser(
            id = userId,
            userListener = UserListener { user ->
                val room = currentUser?.getRoom(roomId)
                room!!.userStore().addOrMerge(user)
                broadcast(UserJoinedRoom(user, room))
            },
            errorListener = ErrorListener { error ->
                broadcastError(error, "User joined a room but I failed getting it: $userId")
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

    private fun handleInitialState(initialState: InitialState) = launch(UI) {
        logger.verbose("Initial state received $initialState")

        val wasExistingCurrentUser = currentUser != null

        if (currentUser != null) {
            currentUser?.presenceSubscription?.unsubscribe()
            currentUser?.updateWithPropertiesOf(initialState.currentUser)
        } else {
            currentUser = CurrentUser(
                apiInstance = apiInstance,
                avatarURL = initialState.currentUser.avatarURL,
                createdAt = initialState.currentUser.createdAt,
                cursors = cursors.await(),
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
        }

        currentUser?.presenceSubscription?.unsubscribe()

        val combinedRoomUserIds = mutableSetOf<String>()
        val roomsForConnection = mutableListOf<Room>()

        initialState.rooms.forEach { room ->
            combinedRoomUserIds.addAll(room.memberUserIds)
            roomsForConnection.add(room)

            currentUser!!.roomStore.addOrMerge(room)
        }

        if (combinedRoomUserIds.size > 0) {
            fetchDetailsForUsers(
                userIds = combinedRoomUserIds,
                onComplete = UsersListener {
                    if (currentUser != null) {
                        updateExistingRooms(roomsForConnection)
                    }
                    subscribePresenceAndCompleteCurrentUser()
                },
                onError = ErrorListener { error ->
                    broadcastError(error, "Failed fetching user details $error")
                })
        } else {
            subscribePresenceAndCompleteCurrentUser()
        }
    }

    private fun broadcastError(error: elements.Error, message: String) {
        logger.error(message)
        broadcast(ErrorOccurred(error))
    }

    private fun subscribePresenceAndCompleteCurrentUser() {
        currentUser?.run {
            broadcast(CurrentUserReceived(this@run))
            launch { presenceEvents.consumeEach {  event -> broadcast(event)} }
        }
    }

    private fun fetchDetailsForUsers(
        userIds: Set<String>,
        onComplete: UsersListener,
        onError: ErrorListener
    ) {

        userStore.fetchUsersWithIds(
            userIds = userIds,
            onComplete = UsersListener { users ->

                currentUser!!.rooms().forEach { room ->
                    room.memberUserIds.forEach { userId ->
                        val user = users.find { it.id == userId }
                        if (user != null) {
                            room.userStore().addOrMerge(user)
                        }
                    }
                }
                onComplete.onUsers(users)
            },
            onFailure = onError
        )
    }

    private fun updateExistingRooms(roomsForConnection: MutableList<Room>) {
        val roomsUserIsNoLongerAMemberOf = currentUser!!.rooms().subtract(roomsForConnection)

        roomsUserIsNoLongerAMemberOf.forEach { room ->
            broadcast(CurrentUserRemovedFromRoom(room.id))
        }
    }

}
