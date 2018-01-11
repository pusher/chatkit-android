package com.pusher.chatkit

import android.os.Handler
import android.os.Looper
import com.pusher.chatkit.ChatManager.Companion.GSON
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import com.google.gson.reflect.TypeToken


class CurrentUser(
        val id: String,
        val createdAt: String,
        var updatedAt: String,
        var name: String?,
        var avatarURL: String?,
        var customData: CustomData?,

        val userStore: GlobalUserStore,
        rooms: List<Room>,
        val apiInstance: Instance,
        val cursorsInstance: Instance,
        val tokenProvider: TokenProvider,
        val tokenParams: ChatkitTokenParams?,
        val logger: Logger

) {
    val mainThread = Handler(Looper.getMainLooper())

    fun updateWithPropertiesOf(newUser: User){
        updatedAt = newUser.updatedAt
        name = newUser.name
        customData = newUser.customData
    }

    var presenceSubscription: PresenceSubscription? = null
    val roomStore: RoomStore

    init {
        val roomMap = ConcurrentHashMap<Int, Room>()
        rooms.forEach { room ->
            roomMap.put(room.id, room)
        }
        roomStore = RoomStore(instance = apiInstance, rooms = roomMap)
    }

    fun rooms(): Set<Room> = roomStore.setOfRooms()
    fun getRoom(id: Int): Room? = roomStore.rooms[id]

    //Room membership related information
    @JvmOverloads fun createRoom(
            name: String,
            isPrivate: Boolean = false,
            userIds: Array<String>? = null,
            onRoomCreatedListener: RoomListener,
            onErrorListener: ErrorListener
    ){
        val roomRequest = RoomCreateRequest(
                name = name,
                private = isPrivate,
                createdById = id,
                userIds = userIds
        )

        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = "/rooms",
                        body = GSON.toJson(roomRequest)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->
                    val room = GSON.fromJson<Room>(response.body()!!.charStream(), Room::class.java)

                    roomStore.addOrMerge(room)
                    populateRoomUserStore(room)

                    mainThread.post { onRoomCreatedListener.onRoom(room) }

                },
                onFailure = { error ->
                    mainThread.post { onErrorListener.onError(error) }
                }
        )
    }

    @JvmOverloads fun getUserRooms(onlyJoinable: Boolean = false, onCompleteListener: RoomsListener){

        val roomListType = object : TypeToken<List<Room>>() {}.getType()
        val path = "/users/$id/rooms"
        apiInstance.request(
                options = RequestOptions(
                    method = "GET",
                    path = path+"?joinable=$onlyJoinable"
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->
                    val rooms = GSON.fromJson<List<Room>>(response.body()!!.string(), roomListType)
                    onCompleteListener.onRooms(rooms)
                },
                onFailure = {
                    logger.error("Tragedy! No rooms could have been returned!")
                }
        )
    }

    fun getJoinableRooms(onCompleteListener: RoomsListener){
        getUserRooms(onlyJoinable = true, onCompleteListener = onCompleteListener)
    }

    @JvmOverloads fun subscribeToRoom(
            room: Room,
            messageLimit: Int = 20,
            listeners: RoomSubscriptionListeners,
            cursorsListeners: CursorsSubscriptionListeners? = null
    ){
        val roomSubscription = RoomSubscription(this, room, userStore, listeners)
        apiInstance.subscribeResuming(
                path = "/rooms/${room.id}?user_id=$id&message_limit=$messageLimit",
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = roomSubscription.subscriptionListeners
        )
        if (cursorsListeners == null) {
            return
        }
        val cursorsSubscription = CursorsSubscription(this, room, userStore, cursorsListeners)
        cursorsInstance.subscribeResuming(
                path = "/cursors/0/rooms/${room.id}/",
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = cursorsSubscription.subscriptionListeners
        )
    }

    private fun populateRoomUserStore(room: Room) {

        room.memberUserIds.forEach { userId ->

            userStore.findOrGetUser(
                    id = userId,
                    userListener = UserListener { user -> room.userStore().addOrMerge(user) },
                    errorListener = ErrorListener {
                        TODO("Not implemented")

                    }
            )
        }
    }

    fun addMessage(
            text: String,
            room: Room,
            onCompleteListener: MessageSentListener,
            onErrorListener: ErrorListener
    ){
        val messageReq = MessageRequest(
                text = text,
                userId = id
        )

        val path = "/rooms/${room.id}/messages"
        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = path,
                        body = GSON.toJson(messageReq)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->
                    val message = GSON.fromJson<MessageSendingResponse>(response.body()!!.charStream(), MessageSendingResponse::class.java)
                    onCompleteListener.onMessage(message.messageId)
                },
                onFailure = { error -> onErrorListener.onError(error)}
        )
    }

    fun addUsers(roomId: Int, users: Array<User>, completeListener: OnCompleteListener, errorListener: ErrorListener) = addUsers(roomId, users.map { id }.toTypedArray(), completeListener, errorListener)
    fun addUsers(roomId: Int, userIds: Array<String>, completeListener: OnCompleteListener, errorListener: ErrorListener) = addOrRemoveUsers("add", roomId, userIds, completeListener, errorListener)

    fun removeUsers(roomId: Int, users: Array<User>, completeListener: OnCompleteListener, errorListener: ErrorListener) = removeUsers(roomId, users.map { id }.toTypedArray(), completeListener, errorListener)
    fun removeUsers(roomId: Int, userIds: Array<String>, completeListener: OnCompleteListener, errorListener: ErrorListener) = addOrRemoveUsers("remove", roomId, userIds, completeListener, errorListener)

    private fun addOrRemoveUsers(
            operation: String,
            roomId: Int,
            userIds: Array<String>,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener){

        val data = object {
            val userIds = userIds
        }

        val path = "/rooms/$roomId/users/$operation"
        apiInstance.request(
                options = RequestOptions(
                        method = "PUT",
                        path = path,
                        body = GSON.toJson(data)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { completeListener.onComplete() },
                onFailure = { error -> errorListener.onError(error) }
        )
    }

    /**
     * Update a room
     * */

    fun updateRoom(
            room: Room,
            name: String? = null,
            isPrivate: Boolean? = null,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener
    ){
        val path = "/rooms/${room.id}"
        val data = UpdateRoomRequest(
                    name = name ?: room.name,
                    isPrivate =  isPrivate ?: room.isPrivate
        )

        apiInstance.request(
                options = RequestOptions(
                        method = "PUT",
                        path = path,
                        body = GSON.toJson(data)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { mainThread.post {  completeListener.onComplete() } },
                onFailure = { error -> mainThread.post { errorListener.onError(error) } }
        )
    }

    data class UpdateRoomRequest(val name: String, val isPrivate: Boolean)

    /**
     * Delete a room
     * */

    fun deleteRoom(room: Room, completeListener: OnCompleteListener, errorListener: ErrorListener) = deleteRoom(room.id, completeListener, errorListener)

    fun deleteRoom(
            roomId: Int,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener
    ){
        val path = "/rooms/$roomId"
        apiInstance.request(
                options = RequestOptions(
                        method = "DELETE",
                        path = path,
                        body = ""
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { mainThread.post {  completeListener.onComplete() } },
                onFailure = { error -> mainThread.post { errorListener.onError(error) } }
        )
    }

    /**
     * Join a room
     * */
    fun joinRoom(room: Room, completeListener: RoomListener, errorListener: ErrorListener) = joinRoom(room.id, completeListener, errorListener)

    fun joinRoom(
            roomId: Int,
            completeListener: RoomListener,
            errorListener: ErrorListener
    ){
        val wrappedCompleteListener = RoomListener { room -> mainThread.post { completeListener.onRoom(room) }}
        val wrappedErrorListener = ErrorListener { error -> mainThread.post { errorListener.onError(error) }}

        val path = HttpUrl.parse("https://pusherplatform.io")!!.newBuilder().addPathSegments("/users/$id/rooms/$roomId/join").build().encodedPath()

        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = path,
                        body = "" //TODO: this is a horrible OKHTTP hack - POST is required to have a body.
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->

                    val room = GSON.fromJson<Room>(response.body()!!.charStream(), Room::class.java)
                    roomStore.addOrMerge(room)
                    populateRoomUserStore(room)
                    wrappedCompleteListener.onRoom(room)

                },
                onFailure = { error -> wrappedErrorListener.onError(error) }
        )
    }


    /**
     * Leave a room
     * */
    fun leaveRoom(room: Room, completeListener: OnCompleteListener, errorListener: ErrorListener) = leaveRoom(room.id, completeListener, errorListener)

    fun leaveRoom(
            roomId: Int,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener
    ){
        val path = HttpUrl.parse("https://pusherplatform.io")!!.newBuilder().addPathSegments("/users/$id/rooms/$roomId/leave").build().encodedPath()

        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = path,
                        body = "" //TODO: this is a horrible OKHTTP hack - POST is required to have a body.
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { completeListener.onComplete() },
                onFailure = { error -> errorListener.onError(error) }
        )
    }

    fun establishPresenceSubscription(listeners: ThreadedUserSubscriptionListeners) {

        presenceSubscription = PresenceSubscription(
                instance = apiInstance,
                path = "/users/$id/presence",
                listeners = listeners,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                userStore = userStore,
                logger = logger
        )
    }
}

data class MessageRequest(val text: String, val userId: String)

data class MessageSendingResponse(val messageId: Int)

data class RoomCreateRequest(
        val name: String,
        val private: Boolean,
        val createdById: String,
        var userIds: Array<String>? = null
)
