package com.pusher.chatkit

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.channels.broadcast
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.rooms.RoomService
import com.pusher.platform.*
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.network.AndroidConnectivityHelper
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Subscription
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel

private const val USERS_PATH = "users"
private const val API_SERVICE_NAME = "chatkit"
private const val CURSOR_SERVICE_NAME = "chatkit_cursors"
private const val SERVICE_VERSION = "v1"
private const val FILES_SERVICE_NAME = "chatkit_files"
private const val PRESENCE_SERVICE_NAME = "chatkit_presence"

class ChatManager @JvmOverloads constructor(
    val instanceLocator: String,
    val userId: String,
    context: Context,
    val tokenProvider: TokenProvider,
    val tokenParams: ChatkitTokenParams? = null,
    logLevel: LogLevel = LogLevel.DEBUG
) {

    companion object {
        val GSON = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }

    private val logger = AndroidLogger(logLevel)

    private val cluster by lazy {
        val splitInstanceLocator = instanceLocator.split(":")
        check(splitInstanceLocator.size == 3) {
            "Locator \'$instanceLocator\' must have the format \'version:cluster:instanceId\'"
        }
        splitInstanceLocator.drop(1).first()
    }
    private val connectivityHelper = AndroidConnectivityHelper(context)
    private val mediaTypeResolver = AndroidMediaTypeResolver()
    private val scheduler = BackgroundScheduler()
    private val mainScheduler = ForegroundScheduler()
    private val baseClient: BaseClient by lazy {
        BaseClient(
            host = "$cluster.pusherplatform.io",
            logger = logger,
            connectivityHelper = connectivityHelper,
            mediaTypeResolver = mediaTypeResolver,
            scheduler = scheduler,
            mainScheduler = mainScheduler
        )
    }
    private var currentUser: CurrentUser? = null

    internal val apiInstance by lazyInstance(API_SERVICE_NAME, SERVICE_VERSION)
    internal val cursorsInstance by lazyInstance(CURSOR_SERVICE_NAME, SERVICE_VERSION)
    internal val filesInstance by lazyInstance(FILES_SERVICE_NAME, SERVICE_VERSION)
    internal val presenceInstance by lazyInstance(PRESENCE_SERVICE_NAME, SERVICE_VERSION)
    internal val userStore by lazy {
        GlobalUserStore(
            apiInstance = apiInstance,
            logger = logger,
            tokenProvider = tokenProvider,
            tokenParams = tokenParams
        )
    }

    init {
        if (tokenProvider is ChatkitTokenProvider) {
            tokenProvider.userId = userId
        }
    }

    fun connect(consumer: (ChatKitEvent) -> Unit): Subscription = UserSubscription(
        userId = userId,
        chatManager = this,
        path = USERS_PATH,
        userStore = userStore,
        tokenProvider = tokenProvider,
        tokenParams = null,
        logger = logger,
        consumeEvent = {
            if (it is CurrentUserReceived) {
                currentUser = it.currentUser
            }
            consumer(it)
        }
    )

    fun messageService(room: Room, user: CurrentUser): MessageService =
        MessageService(room, user, this)

    fun roomService( user: CurrentUser): RoomService =
        RoomService(user)

    private fun lazyInstance(serviceName: String, serviceVersion: String) = lazy {
        Instance(
            locator = instanceLocator,
            serviceName = serviceName,
            serviceVersion = serviceVersion,
            logger = logger,
            baseClient = baseClient,
            connectivityHelper = connectivityHelper,
            scheduler = scheduler,
            mainThreadScheduler = mainScheduler,
            mediatypeResolver = mediaTypeResolver
        )
    }

}

@UsesCoroutines
fun ChatManager.connectAsync(): ReceiveChannel<ChatKitEvent> =
    broadcast { connect { event -> offer(event) } }

data class Message(
    val id: Int,
    val userId: String,
    val roomId: Int,
    val text: String? = null,
    val attachment: Attachment? = null,
    val createdAt: String,
    val updatedAt: String,

    var user: User?,
    var room: Room?
)

data class Attachment(
    @Transient var fetchRequired: Boolean = false,
    @SerializedName("resource_link") val link: String,
    val type: String
)

data class Cursor(
    val userId: String,
    val roomId: Int,
    val type: Int,
    val position: Int,
    val updatedAt: String,

    var user: User?,
    var room: Room?
)

data class ChatEvent(
    val eventName: String,
    val userId: String? = null,
    val timestamp: String,
    val data: JsonElement
)

typealias CustomData = MutableMap<String, String>

sealed class ChatKitEvent

data class CurrentUserReceived(val currentUser: CurrentUser) : ChatKitEvent()
data class ErrorOccurred(val error: elements.Error) : ChatKitEvent()
data class CurrentUserAddedToRoom(val room: Room) : ChatKitEvent()
data class CurrentUserRemovedFromRoom(val roomId: Int) : ChatKitEvent()
data class RoomDeleted(val roomId: Int) : ChatKitEvent()
data class RoomUpdated(val room: Room) : ChatKitEvent()
data class UserPresenceUpdated(val user: User, val newPresence: User.Presence) : ChatKitEvent()
data class UserJoinedRoom(val user: User, val room: Room) : ChatKitEvent()
data class UserLeftRoom(val user: User, val room: Room) : ChatKitEvent()
