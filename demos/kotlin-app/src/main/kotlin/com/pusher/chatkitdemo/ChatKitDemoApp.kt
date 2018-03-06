package com.pusher.chatkitdemo

import android.app.Application
import android.content.Context
import android.support.v4.app.Fragment
import com.pusher.chatkit.*
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkitdemo.BuildConfig.*
import com.pusher.chatkitdemo.parallel.lazyBroadcast
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import kotlinx.coroutines.experimental.channels.*

val Context.app: ChatKitDemoApp
    get() = when (applicationContext) {
        null -> throw IllegalStateException("Application context is null")
        is ChatKitDemoApp -> applicationContext as ChatKitDemoApp
        else -> throw IllegalStateException("Application context ($applicationContext) is not ${nameOf<ChatKitDemoApp>()}")
    }

val Fragment.app: ChatKitDemoApp
    get() = context!!.app

class ChatKitDemoApp : Application() {

    private val tokenProvider = ChatkitTokenProvider(TOKEN_PROVIDER_ENDPOINT, USER_ID)

    val logger : Logger by lazy { AndroidLogger(LogLevel.VERBOSE) }

    val chat: ChatManager by lazy {
        ChatManager(
            instanceLocator = INSTANCE_LOCATOR,
            userId = USER_ID,
            context = applicationContext,
            tokenProvider = tokenProvider,
            logLevel = LogLevel.VERBOSE
        )
    }

    private val events by lazy { chat.connectAsync() }

    private val currentUserBroadcast by lazyBroadcast<CurrentUser> {
        events.map { event -> (event as? CurrentUserReceived)?.currentUser }
            .filterNotNull()
            .consumeEach { offer(it) }
    }

    val currentUser: ReceiveChannel<CurrentUser>
        get() = currentUserBroadcast.openSubscription()


    val rooms: ReceiveChannel<RoomService>
        get() = currentUser.map { chat.roomService(it) }

    fun messageServiceFor(room: Room): ReceiveChannel<MessageService> =
        currentUser.map { user -> chat.messageService(room, user) }

}
