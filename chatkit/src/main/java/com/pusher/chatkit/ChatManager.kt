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
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.rooms.RoomStateMachine
import com.pusher.chatkit.users.HasUser
import com.pusher.chatkit.users.UserService
import com.pusher.platform.*
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.network.AndroidConnectivityHelper
import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.network.Promise
import com.pusher.platform.network.Promise.PromiseContext
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Subscription
import elements.asSystemError
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlin.properties.Delegates

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
    tokenProvider: TokenProvider,
    val tokenParams: ChatkitTokenParams? = null,
    logLevel: LogLevel = LogLevel.DEBUG
) {

    val tokenProvider: TokenProvider = DebounceTokenProvider(tokenProvider)

    companion object {
        val GSON: Gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }

    internal val logger = AndroidLogger(logLevel)

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

    // TODO: report when relevant error occurs (i.e.: failed to connect)
    private var currentUserPromiseContext: PromiseContext<Result<CurrentUser, Error>> by Delegates.notNull()

    val currentUser: Promise<Result<CurrentUser, Error>> = Promise.promise {
        currentUserPromiseContext = this
    }

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

    fun connect(consumer: (ChatManagerEvent) -> Unit): Subscription = UserSubscription(
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

    private fun ChatManagerEvent.handleEvent() {
        when (this) {
            is CurrentUserReceived -> currentUserPromiseContext.report(currentUser.asSuccess())
            is ErrorOccurred -> logger.error(error.reason, error.asSystemError())
            is RoomDeleted -> roomStore -= roomId
            is RoomUpdated -> roomStore += room
            is CurrentUserRemovedFromRoom -> currentUser.onReady { result ->
                result.map { currentUser -> roomStore[roomId]?.removeUser(currentUser.id) }
            }
            is CurrentUserAddedToRoom -> currentUser.onReady { result ->
                result.map { currentUser -> room.addUser(currentUser.id) }
            }
        }
    }

    fun messageService(room: Room): MessageService =
        MessageService(room, this)

    fun roomService(): Promise<Result<RoomService, Error>> =
        currentUser.mapResult { user -> RoomService(user, this) }

    fun userService(): UserService =
        UserService(this)

    fun roomStateMachine() : RoomStateMachine =
        RoomStateMachine(BackgroundScheduler(), this)

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
    internal fun doPost(path: String, body: String = ""): OkHttpResponsePromise =
        doRequest("POST", path, body)

    internal fun doGet(path: String): OkHttpResponsePromise =
        doRequest("GET", path, null)

    private fun doRequest(method: String, path: String, body: String?): OkHttpResponsePromise =
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
fun ChatManager.connectAsync(): ReceiveChannel<ChatManagerEvent> =
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

sealed class ChatManagerEvent {

    companion object {
        fun onError(error: Error): ChatManagerEvent = ErrorOccurred(error)
        fun onUserJoinedRoom(user: User, room: Room): ChatManagerEvent = UserJoinedRoom(user, room)
    }

}

data class CurrentUserReceived(val currentUser: CurrentUser) : ChatManagerEvent()
data class ErrorOccurred(val error: elements.Error) : ChatManagerEvent()
data class CurrentUserAddedToRoom(val room: Room) : ChatManagerEvent()
data class CurrentUserRemovedFromRoom(val roomId: Int) : ChatManagerEvent()
data class RoomDeleted(val roomId: Int) : ChatManagerEvent()
data class RoomUpdated(val room: Room) : ChatManagerEvent()
data class UserPresenceUpdated(val user: User, val newPresence: User.Presence) : ChatManagerEvent()
data class UserJoinedRoom(val user: User, val room: Room) : ChatManagerEvent()
data class UserLeftRoom(val user: User, val room: Room) : ChatManagerEvent()
object NoEvent : ChatManagerEvent()

private class DebounceTokenProvider(
    val original: TokenProvider
) : TokenProvider {

    private var pending: Promise<Result<String, Error>>? = null

    override fun fetchToken(tokenParams: Any?): Promise<Result<String, Error>> = synchronized(this) {
        pending ?: original.fetchToken(tokenParams).also { pending = it }
    }

    override fun clearToken(token: String?) = synchronized(this) {
        original.clearToken(token)
        pending = null
    }

}
