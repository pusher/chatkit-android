package com.pusher.chatkitdemo

import android.app.Application
import android.content.Context
import android.support.v4.app.Fragment
import com.pusher.chatkit.*
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkitdemo.BuildConfig.*
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Promise
import com.pusher.platform.network.await
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch

val Context.app: ChatKitDemoApp
    get() = when (applicationContext) {
        null -> throw IllegalStateException("Application context is null")
        is ChatKitDemoApp -> applicationContext as ChatKitDemoApp
        else -> throw IllegalStateException("Application context ($applicationContext) is not ${nameOf<ChatKitDemoApp>()}")
    }

val Fragment.app: ChatKitDemoApp
    get() = context!!.app

class ChatKitDemoApp : Application() {

    companion object {
        private var maybeApp: ChatKitDemoApp? = null
        val app get() = checkNotNull(maybeApp)
    }

    init {
        maybeApp = this
    }

    private val tokenProvider: TokenProvider
        get() {
            val userId = userPreferences.userId
            val token = userPreferences.token
            checkNotNull(userId) { "No user id available" }
            checkNotNull(token) { "No token available" }
            val endpoint = "$TOKEN_PROVIDER_ENDPOINT?user=$userId&token=$token"
            return ChatkitTokenProvider(endpoint, USER_ID)
        }

    val logger: Logger by lazy { AndroidLogger(LogLevel.VERBOSE) }
    val userPreferences by lazy { UserPreferences(this) }

    private val chat: ChatManager by lazy {
        ChatManager(
            instanceLocator = INSTANCE_LOCATOR,
            userId = userPreferences.userId ?: USER_ID,
            context = applicationContext,
            tokenProvider = tokenProvider,
            logLevel = LogLevel.VERBOSE
        )
    }

    val events by lazy { chat.connectAsync() }

    private val currentUserBroadcast: Promise<Result<CurrentUser, Error>> by lazy {
        Promise.promise<Result<CurrentUser, Error>> {
            launch {
                events.consumeEach { event ->
                    when (event) {
                        is CurrentUserReceived -> report(event.currentUser.asSuccess())
                        is ErrorOccurred -> report(event.error.asFailure())
                    }
                }
            }
        }
    }

    suspend fun currentUser(): Result<CurrentUser, Error> =
        currentUserBroadcast.await()

    suspend fun rooms(): Result<RoomService, Error> =
        currentUser().map { chat.roomService(it) }

    suspend fun messageServiceFor(room: Room): Result<MessageService, Error> =
        currentUser().map { chat.messageService(room, it) }

}
