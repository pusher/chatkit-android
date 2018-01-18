package com.pusher.chatkit

import android.os.Handler
import android.os.Looper
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.suspendCoroutine

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
        val apiInstance: Instance,
        val cursorsInstance: Instance,
        val filesInstance: Instance,
        path: String,
        val userStore: GlobalUserStore,
        val tokenProvider: TokenProvider,
        val tokenParams: ChatkitTokenParams?,
        val logger: Logger,
        val listeners: ThreadedUserSubscriptionListeners
) {

    var subscription: Subscription? = null
    private val cursors: Deferred<ConcurrentHashMap<Int, Cursor>> = async { getCursors() }
    lateinit var headers: Headers

    init {
        subscription = apiInstance.subscribeResuming(
                path = path,
                listeners = SubscriptionListeners(
                        onOpen = { headers ->
                            logger.warn("OnOpen $headers")
                            this.headers = headers
                        },
                        onEvent = { event ->
                            logger.warn("Event $event")
                            handleEvent(event)
                        },
                        onError = { error ->
                            logger.warn("Error $error")
                            listeners.onError(error)
                        },
                        onSubscribe = {
                            logger.warn("Subscription established.")
                        },
                        onRetrying = {
                            logger.warn("Subscription lost. Trying again.")
                        },
                        onEnd = {
                            error ->
                            logger.warn("Subscription ended with: $error")

                        }
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams
        )
    }

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
                    listeners.onError(error)
                    cont.resume(cursorsByRoom)
                }
        )
    }

    fun handleEvent(event: SubscriptionEvent) {

        logger.warn("Handle event: $event")

        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)
        when(chatEvent.eventName){
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
            else -> { throw Error("Invalid event name: ${chatEvent.eventName}") }
        }
    }

    private fun handleUserLeft(userId: String, roomId: Int) {
        userStore.findOrGetUser(
                id = userId,
                userListener = UserListener { user ->
                    val room = currentUser?.getRoom(roomId)
                    room!!.removeUser(userId)
                    listeners.userLeft(user, room)
                },
                errorListener = ErrorListener { error ->
                    logger.warn("User left a room but I failed getting it: $userId")
                    listeners.onError(error)
                })
    }

    private fun handleUserJoined(userId: String, roomId: Int) {

        userStore.findOrGetUser(
                id = userId,
                userListener = UserListener { user ->
                    val room = currentUser?.getRoom(roomId)
                    room!!.userStore().addOrMerge(user)

                    listeners.userJoined(user, room)
                },
                errorListener = ErrorListener { error ->
                    logger.warn("User joined a room but I failed getting it: $userId")
                    listeners.onError(error)
                })
    }

    private fun handleRoomDeleted(roomId: Int) {
        currentUser?.roomStore?.rooms?.remove(roomId)
        listeners.roomDeleted(roomId)
    }

    private fun handleRoomUpdated(room: Room) {
        currentUser?.roomStore?.addOrMerge(room)
        listeners.roomUpdated(room)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun handleRemovedFromRoom(roomId: Int) {
        currentUser?.roomStore?.rooms?.remove(roomId)
        listeners.removedFromRoom(roomId)
    }

    private fun handleAddedToRoom(room: Room){
        currentUser?.roomStore?.addOrMerge(room)
        listeners.addedToRoom(room)
    }



    private var currentUser: CurrentUser? = null

    private fun handleInitialState(initialState: InitialState) = launch {
        logger.verbose("Initial state received $initialState")

        var wasExistingCurrentUser = currentUser != null

        if(currentUser != null){
            currentUser?.presenceSubscription?.unsubscribe()
            currentUser?.updateWithPropertiesOf(initialState.currentUser)
        }
        else{
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
                    tokenProvider = tokenProvider,
                    tokenParams = tokenParams,
                    updatedAt = initialState.currentUser.updatedAt,
                    userStore = userStore
            )
        }

        currentUser?.presenceSubscription?.unsubscribe()
        currentUser?.presenceSubscription = null

        val combinedRoomUserIds = mutableSetOf<String>()
        val roomsForConnection = mutableListOf<Room>()

        initialState.rooms.forEach { room ->
            combinedRoomUserIds.addAll(room.memberUserIds)
            roomsForConnection.add(room)

            currentUser!!.roomStore.addOrMerge(room)
        }

        if(combinedRoomUserIds.size > 0){
            fetchDetailsForUsers(
                    userIds = combinedRoomUserIds,
                    onComplete = UsersListener {
                        if(wasExistingCurrentUser){
                            updateExistingRooms(roomsForConnection)
                        }
                        subscribePresenceAndCompleteCurrentUser()
                    },
                    onError = ErrorListener { error ->
                        logger.error("Failed fetching user details $error")
                        listeners.onError(error)
                    })
        }
        else {
            subscribePresenceAndCompleteCurrentUser()
        }
    }

    private fun subscribePresenceAndCompleteCurrentUser() {

        Handler(Looper.getMainLooper()).post {
            currentUser?.establishPresenceSubscription(listeners)

            listeners.currentUserListener(currentUser!!)
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
                            if(user != null){
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
            listeners.removedFromRoom(room.id)
        }
    }
}