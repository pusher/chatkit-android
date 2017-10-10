package com.pusher.chatkit

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.pusher.platform.Cancelable
import com.pusher.platform.Instance
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.tokenProvider.TokenProvider

class ChatManager(
        instanceId: String,
        context: Context,
        val tokenProvider: TokenProvider? = null,
        logLevel: LogLevel = LogLevel.DEBUG
){

    companion object {
        val GSON = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
    }

    var currentUser: CurrentUser? = null
    val serviceName = "chatkit"
    val serviceVersion = "v1"
    val logger = AndroidLogger(logLevel)

    val instance = Instance(
            instanceId = instanceId,
            serviceName = serviceName,
            serviceVersion = serviceVersion,
            context = context,
            logger = logger
    )

    val userStore = GlobalUserStore(instance)

    var userSubscription: UserSubscription? = null //Initialised when connect() is called.

    fun connect(
            onCurrentUser: CurrentUserListener,
            onError: ErrorListener
    ){
        val path = "users"
        this.userSubscription = UserSubscription(
                instance = instance,
                path = path,
                userStore = userStore,
                tokenProvider = tokenProvider,
                tokenParams = null,
//                tokenParams = ChatkitTokenParams(),
                logger = logger,
                onCurrentUser = CurrentUserListener { user -> onCurrentUser.onCurrentUser(user) },
                onError = ErrorListener { error -> onError.onError(error) }
        )
    }
}

//
//enum class EventType(type: String){
//    INITIAL_STATE("initial_state"),
//    ADDED_TO_ROOM("added_to_room"),
//    REMOVED_FROM_ROOM("removed_from_room"),
//    NEW_MESSAGE("new_message"),
//    ROOM_UPDATED("room_updated"),
//    ROOM_DELETED("room_deleted"),
//    USER_JOINED("user_joined"),
//    USER_LEFT("user_left")
//}

data class InitialState(
        val rooms: List<Room>, //TODO: might need to use a different subsctructure for this
        val currentUser: CurrentUser
)

data class AddedToRoom(
        val room: Room
)

data class RemovedFromRoomPayload(
        val roomId: Int
)

data class Message(
        val id: Int,
        val userId: String,
        val roomId: Int,
        val text: String,
        val createdAt: String,
        val updatedAt: String
)

data class RoomUpdated(
        val room: Room
)

data class RoomDeleted(
        val roomId: Int
)

data class UserJoined(
        val roomId: Int,
        val userId: String
)

data class UserLeft(
        val roomId: Int,
        val userId: String
)


data class ChatEvent(val eventName: String, val userId: String? = null, val timestamp: String, val data: JsonElement)

class GlobalUserStore(instance: Instance) {

}

class RoomStore(instance: Instance, rooms: MutableList<Room>) {

}

class UserStore {

}

typealias CustomData = MutableMap<String, String>

data class Room(
        val id: Int,
        val createdById: String,
        val name: String,
        val createdAt: String,
        val updatedAta: String,
        val memberUserIds: List<String>

)
