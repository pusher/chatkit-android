package com.pusher.chatkit

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.pusher.platform.Instance
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.tokenProvider.TokenProvider

class ChatManager(
        instanceId: String,
        context: Context,
        val tokenProvider: TokenProvider? = null,
        val tokenParams: ChatkitTokenParams? = null,
        logLevel: LogLevel = LogLevel.DEBUG
){

    companion object {
        val GSON = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
    }

    class Builder {

        private var instanceId: String? = null
        private var context: Context? = null
        private var tokenProvider: TokenProvider? = null
        private var tokenParams: ChatkitTokenParams? = null
        private var logLevel = LogLevel.DEBUG

        fun instanceId(instanceId: String): Builder{
            this.instanceId = instanceId
            return this
        }

        fun context(context: Context): Builder{
            this.context = context
            return this
        }

        fun tokenProvider(tokenProvider:  TokenProvider): Builder{
            this.tokenProvider = tokenProvider
            return this
        }

        fun tokenParams(tokenParams: ChatkitTokenParams): Builder{
            this.tokenParams = tokenParams
            return this
        }

        fun logLevel(logLevel: LogLevel): Builder{
            this.logLevel = logLevel
            return this
        }

        fun build(): ChatManager {
            if(instanceId == null){
                throw Error("setInstanceId() not called")
            }
            if(context == null){
                throw Error("setContext() not called")
            }
            if(tokenProvider == null){
                throw Error("setTokenProvider() not called")
            }

            return ChatManager(instanceId!!, context!!, tokenProvider, tokenParams, logLevel)
        }
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

    val userStore = GlobalUserStore(
            instance = instance,
            logger = logger,
            tokenProvider = tokenProvider,
            tokenParams = tokenParams
    )

    var userSubscription: UserSubscription? = null //Initialised when connect() is called.

    fun connect(listener: UserSubscriptionListener){
        val mainThreadListeners = ThreadedUserSubscriptionListeners.from(
                listener = listener,
                thread = Handler(Looper.getMainLooper()))

        val path = "users"
        this.userSubscription = UserSubscription(
                instance = instance,
                path = path,
                userStore = userStore,
                tokenProvider = tokenProvider!!,
                tokenParams = null,
                logger = logger,
                listeners = mainThreadListeners
        )
    }
}

class ThreadedUserSubscriptionListeners
private constructor(
        val currentUserListener: (CurrentUser) -> Unit,
        val onError: (elements.Error) -> Unit,
        val removedFromRoom: (Int) -> Unit,
        val addedToRoom: (Room) -> Unit,
        val roomUpdated: (Room) -> Unit,
        val roomDeleted: (Int) -> Unit,
        val userJoined: (User, Room) -> Unit,
        val userLeft: (User, Room) -> Unit,
        val userCameOnline: (User) -> Unit,
        var userWentOffline: (User) -> Unit
)
{
    companion object {
        fun from(listener: UserSubscriptionListener, thread: Handler): ThreadedUserSubscriptionListeners{
            return ThreadedUserSubscriptionListeners(
                    currentUserListener = { user -> thread.post { listener.currentUserReceived(user) }},
                    onError = { error -> thread.post {  listener.onError(error) }},
                    removedFromRoom = { roomId -> thread.post { listener.removedFromRoom(roomId) }},
                    addedToRoom = { room -> thread.post { listener.addedToRoom(room) }},
                    roomUpdated = { room -> thread.post { listener.roomUpdated(room) }},
                    roomDeleted = { roomId -> thread.post { listener.roomDeleted(roomId) }},
                    userJoined = { user, room -> thread.post { listener.userJoined(user, room) }},
                    userLeft = { user, room -> thread.post { listener.userLeft(user, room) }},
                    userCameOnline = { user -> thread.post { listener.userCameOnline(user) }},
                    userWentOffline = { user -> thread.post { listener.userWentOffline(user) }}
            )
        }
    }
}


data class Message(
        val id: Int,
        val userId: String,
        val roomId: Int,
        val text: String,
        val createdAt: String,
        val updatedAt: String,

        var user: User?,
        var room: Room?
)

data class ChatEvent(
        val eventName: String,
        val userId: String? = null,
        val timestamp: String,
        val data: JsonElement)


typealias CustomData = MutableMap<String, String>

