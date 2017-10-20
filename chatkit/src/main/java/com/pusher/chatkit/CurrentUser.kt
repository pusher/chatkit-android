package com.pusher.chatkit

import com.pusher.chatkit.ChatManager.Companion.GSON
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Subscription
import java.util.concurrent.ConcurrentHashMap

class CurrentUser(
        val id: String,
        val createdAt: String,
        var updatedAt: String,
        var name: String?,
        var avatarURL: String?,
        var customData: CustomData?,

        val userStore: GlobalUserStore,
        rooms: List<Room>,
        val instance: Instance,
        val tokenProvider: TokenProvider,
        val tokenParams: ChatkitTokenParams?

) {
    fun updateWithPropertiesOf(newUser: User){
        updatedAt = newUser.updatedAt
        name = newUser.name
        customData = newUser.customData
    }

    var presenceSubscription: Subscription? = null
    val roomStore: RoomStore

    init {
        val roomMap = ConcurrentHashMap<Int, Room>()
        rooms.forEach { room ->
            roomMap.put(room.id, room)
        }
        roomStore = RoomStore(instance = instance, rooms = roomMap)
    }

    fun rooms(): Set<Room> = roomStore.rooms()

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
                isPrivate = isPrivate,
                createdById = id,
                userIds = userIds
        )

        instance.request(
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

                    onRoomCreatedListener.onRoom(room)

                },
                onFailure = { error ->
                    onErrorListener.onError(error)
                }
        )
    }

    @JvmOverloads fun subscribeToRoom(
            room: Room,
            messageLimit: Int = 20,
            listeners: RoomSubscriptionListeners
    ){

        val path = "/rooms/${room.id}?user_id=$id&message_limit=$messageLimit"

        val roomSubscription = RoomSubscription(this, room, userStore, listeners)

        instance.subscribeResuming(
                path = path,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = roomSubscription.subscriptionListeners
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

    fun addUsers(){
        TODO()
    }

    fun removeUsers(){
        TODO()
    }


    /**
     * Update a room
     * */

    fun updateRoom(
            roomId: Int,
            name: String? = null,
            isPrivate: Boolean = false,
            onComplete: Any
    ){
        TODO()
    }

    /**
     * Delete a room
     * */
    fun deleteRoom(
            roomId: Int,
            onComplete: Any
    ){
        TODO()
    }

    /**
     * Join a room
     * */
    fun joinRoom(
            roomId: Int,
            onComplete: Any
    ){
        TODO()
    }

    /**
     * Leave a room
     * */
    fun leaveRoom(
            roomId: Int,
            onComplete: Any
    ){
        TODO()
    }

    //TODO: All the other shit - typealias, messages for room, etc...

}

data class RoomSubscriptionListeners @JvmOverloads constructor(
        val onNewMessage: MessageListener = MessageListener {  },
        val userStartedTyping: UserListener = UserListener {  },
        val userStoppedTyping: UserListener = UserListener {  },
        val userJoined: UserListener = UserListener {  },
        val userLeft: UserListener = UserListener {  },
        val userCameOnline: UserListener = UserListener {  },
        val userWentOffline: UserListener = UserListener {  },
        val usersUpdated: ErrorListener = ErrorListener {  }, //TODO
        val errorListener: ErrorListener = ErrorListener {  }
)

data class RoomCreateRequest(
        val name: String,
        val isPrivate: Boolean,
        val createdById: String,
        var userIds: Array<String>? = null
)
