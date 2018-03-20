package com.pusher.chatkit

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.pusher.annotations.UsesCoroutines
import com.pusher.chatkit.channels.broadcast
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.rooms.HasRoom
import com.pusher.chatkit.rooms.RoomPromiseResult
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.users.HasUser
import com.pusher.chatkit.users.UserService
import com.pusher.platform.*
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.network.AndroidConnectivityHelper
import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.network.Promise
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.Error
import elements.Subscription
import elements.asSystemError
import kotlinx.coroutines.experimental.channels.ReceiveChannel

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
        val GSON: Gson = GsonBuilder()
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

    internal val userStore by lazy { GlobalUserStore() }
    internal val roomStore by lazy { RoomStore() }

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
        consumeEvent = { event ->
            event.handleEvent()
            consumer(event)
        }
    )

    private fun ChatKitEvent.handleEvent() {
        when (this) {
            is CurrentUserReceived -> this@ChatManager.currentUser = currentUser
            is ErrorOccurred -> logger.error(error.reason, error.asSystemError())
            is RoomDeleted -> roomStore -= roomId
            is RoomUpdated -> roomStore += room
            is CurrentUserRemovedFromRoom -> currentUser?.id?.let { id -> roomStore[roomId]?.removeUser(id) }
        }
    }

    fun messageService(room: Room, user: CurrentUser): MessageService =
        MessageService(room, user, this)

    fun roomService(user: CurrentUser): RoomService =
        RoomService(user, this)

    fun userService(): UserService =
        UserService(this)

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

    @JvmOverloads
    internal fun doPost(path : String, body: String = ""): OkHttpResponsePromise =
        doRequest("POST", path, body)

    internal fun doGet(path : String): OkHttpResponsePromise =
        doRequest("GET", path, null)

    private fun doRequest(method : String, path : String, body: String?) : OkHttpResponsePromise =
        apiInstance.request(
            options = RequestOptions(
                method = method,
                path = path,
                body = body
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams
        )

}

@UsesCoroutines
fun ChatManager.connectAsync(): ReceiveChannel<ChatKitEvent> =
    broadcast { connect { event -> offer(event) } }

data class Message(
    val id: Int,
    override val userId: String,
    override val roomId: Int,
    val text: String? = null,
    val attachment: Attachment? = null,
    val createdAt: String,
    val updatedAt: String
) : HasRoom, HasUser

data class Attachment(
    @Transient var fetchRequired: Boolean = false,
    @SerializedName("resource_link") val link: String,
    val type: String
)

data class Cursor(
    override val userId: String,
    override val roomId: Int,
    val type: Int,
    val position: Int,
    val updatedAt: String
) : HasUser, HasRoom

data class ChatEvent(
    val eventName: String,
    override val userId: String = "",
    val timestamp: String,
    val data: JsonElement
) : HasUser

typealias CustomData = MutableMap<String, String>

sealed class ChatKitEvent {

    companion object {
        fun onError(error: Error): ChatKitEvent = ErrorOccurred(error)
        fun onUserJoinedRoom(user: User, room: Room): ChatKitEvent = UserJoinedRoom(user, room)
    }

}

data class CurrentUserReceived(val currentUser: CurrentUser) : ChatKitEvent()
data class ErrorOccurred(val error: elements.Error) : ChatKitEvent()
data class CurrentUserAddedToRoom(val room: Room) : ChatKitEvent()
data class CurrentUserRemovedFromRoom(val roomId: Int) : ChatKitEvent()
data class RoomDeleted(val roomId: Int) : ChatKitEvent()
data class RoomUpdated(val room: Room) : ChatKitEvent()
data class UserPresenceUpdated(val user: User, val newPresence: User.Presence) : ChatKitEvent()
data class UserJoinedRoom(val user: User, val room: Room) : ChatKitEvent()
data class UserLeftRoom(val user: User, val room: Room) : ChatKitEvent()
object NoEvent: ChatKitEvent()
