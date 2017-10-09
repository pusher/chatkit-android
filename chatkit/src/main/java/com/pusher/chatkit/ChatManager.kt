package com.pusher.chatkit

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.FieldNamingStrategy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.pusher.platform.Cancelable
import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.tokenProvider.TokenProvider
import elements.SubscriptionEvent

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
            context = context
    )

    val userStore = GlobalUserStore(instance)

    var userSubscription: UserSubscription? = null //Initialised when connect() is called.

    fun connect(
            onCurrentUser: CurrentUserListener,
            onError: ErrorListener
    ){
        val path = "/users"

        instance.subscribeResuming(
                path = path,
                listeners = SubscriptionListeners(
                        onEvent = { event ->
//                            val chatEvent =
                        },
                        onError = { error -> },
                        onEnd = { endEvent -> },
                        onOpen = { headers ->  }, //Ignored
                        onRetrying = {}, //Ignored
                        onSubscribe = {} //Ignored
                )
        )

        this.userSubscription = UserSubscription(
                instance = instance,
                path = path,
                userStore = userStore,
                onCurrentUser = CurrentUserListener { user -> }, //TODO
                onError = ErrorListener { error -> } //TODO
        )



    }
}

data class InitialStatePayload(
        val rooms: List<Room>, //TODO: might need to use a different subsctructure for this
        val currentUser: CurrentUser
)

data class ChatEvent(val eventName: String, val userId: String? = null, val timestamp: String, val data: JsonElement)

class UserSubscription(
        instance: Instance,
        path: String,
        userStore: GlobalUserStore,
        onCurrentUser: CurrentUserListener,
        onError: ErrorListener
) {

    val subscription = instance.subscribeResuming(
            path = path,
            listeners = SubscriptionListeners(
                    onEvent = { event -> handleEvent(event) },
                    onError = { error -> onError.onError(error) }
            )
    )

    fun handleEvent(event: SubscriptionEvent) {

    }

    init{

    }

}

class GlobalUserStore(instance: Instance) {

}


class ChatkitTokenProvider: TokenProvider {
    override fun clearToken(token: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchToken(tokenParams: Any?, onSuccess: (String) -> Unit, onFailure: (elements.Error) -> Unit): Cancelable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class CurrentUser(
        val id: String,
        val createdAt: String,
        val updatedAt: String,

        val name: String?,
        val avatarURL: String?,
        val customData: CustomData?,

        val rooms: MutableList<Room> = ArrayList<Room>(),
        val instance: Instance,
        val userStore: UserStore
) {
    val roomStore = RoomStore(instance = instance, rooms = rooms)

    init {
        TODO()
    }

    //Room membership related information
    fun createRoom(){
        TODO()
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

class RoomStore(instance: Instance, rooms: MutableList<Room>) {

}

class UserStore {

}

typealias CustomData = MutableMap<String, Any>

data class Room(
        val id: Int,
        val createdById: String,
        val name: String,
        val createdAt: String,
        val updatedAta: String,
        val memberUserIds: List<String>

)
