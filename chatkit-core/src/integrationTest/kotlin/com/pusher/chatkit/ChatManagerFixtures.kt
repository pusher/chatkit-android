package com.pusher.chatkit

import com.pusher.SdkInfo
import com.pusher.chatkit.test.NoAppHooks
import com.pusher.chatkit.test.NoPushNotificationFactory
import com.pusher.chatkit.test.insecureOkHttpClient
import com.pusher.platform.MediaTypeResolver
import com.pusher.platform.PlatformDependencies
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import okhttp3.OkHttpClient

const val MAX_LOG_LENGTH = 3000

class TestDependencies : PlatformDependencies {
    override val logger: Logger = object : Logger {
        private val timeFormatter = DateTimeFormatter.ISO_TIME
        override fun verbose(message: String, error: Error?) = Unit
        override fun debug(message: String, error: Error?) = log("D", message.take(MAX_LOG_LENGTH), error)
        override fun info(message: String, error: Error?) = log("I", message.take(MAX_LOG_LENGTH), error)
        override fun warn(message: String, error: Error?) = log("W", message.take(MAX_LOG_LENGTH), error)
        override fun error(message: String, error: Error?) = log("E", message.take(MAX_LOG_LENGTH), error)
        private fun log(type: String, message: String, error: Error?) =
                println("${ts()} $type: $message ${error?.let { "\n" + it }
                        ?: ""}".take(MAX_LOG_LENGTH))

        private fun ts() = timeFormatter.format(LocalDateTime.now())
    }
    override val mediaTypeResolver: MediaTypeResolver = object : MediaTypeResolver {
        override fun fileMediaType(file: File): String? = "image/jif"
    }
    override val sdkInfo: SdkInfo = SdkInfo(
            product = "SynchronousChatManager Integration Tests",
            language = "Spek",
            platform = "JUnit",
            sdkVersion = "test"
    )
}

class TestChatkitDependencies(
    override val tokenProvider: TokenProvider,
    platformDependencies: PlatformDependencies = TestDependencies()
) : ChatkitDependencies, PlatformDependencies by platformDependencies {
    override val okHttpClient: OkHttpClient = insecureOkHttpClient.newBuilder().apply {
        addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("Connection", "close").build())
        }
    }.build()
    override val pushNotifications = NoPushNotificationFactory()
    override val appHooks = NoAppHooks()
}

// val SynchronousCurrentUser.generalRoom
//    get() = rooms.find { it.name == Rooms.GENERAL } ?: error("Could not find room general")
//
// private val managers = ConcurrentLinkedQueue<SynchronousChatManager>()
//
// fun chatFor(userName: String) = SynchronousChatManager(
//        instanceLocator = INSTANCE_LOCATOR,
//        userId = userName,
//        dependencies = TestChatkitDependencies(
//                tokenProvider = TestTokenProvider(INSTANCE_ID, userName, AUTH_KEY_ID, AUTH_KEY_SECRET)
//        )
// ).also { managers += it }
//
// fun closeChatManagers() {
//    for (manager in managers) {
//        manager.close()
//    }
//    managers.clear()
// }

fun <A> Result<A, elements.Error>.assumeSuccess(): A = when (this) {
    is Result.Success -> value
    is Result.Failure -> error("Failure: $error")
}
