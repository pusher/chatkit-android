package com.pusher.chatkit

import com.pusher.SdkInfo
import com.pusher.chatkit.test.insecureOkHttpClient
import com.pusher.platform.*
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Wait
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class TestDependencies : PlatformDependencies {
    override val logger: Logger = object : Logger {
        override fun verbose(message: String, error: Error?) = log("V", message.take(200), error)
        override fun debug(message: String, error: Error?) = log("D", message.take(200), error)
        override fun info(message: String, error: Error?) = log("I", message.take(200), error)
        override fun warn(message: String, error: Error?) = log("W", message.take(200), error)
        override fun error(message: String, error: Error?) = log("E", message.take(200), error)
        private fun log(type: String, message: String, error: Error?) =
            println("$type: $message ${error?.let { "\n" + it } ?: ""}".take(200))
    }
    override val mediaTypeResolver: MediaTypeResolver = object : MediaTypeResolver {
        override fun fileMediaType(file: File): String? = "image/jif"
    }
    override val sdkInfo: SdkInfo = SdkInfo(
        product = "ChatManager Integration Tests",
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
}

val CurrentUser.generalRoom
    get() = rooms.find { it.name == Rooms.GENERAL } ?: error("Could not find room general")

private val managers = mutableListOf<ChatManager>()

fun chatFor(userName: String) = ChatManager(
    instanceLocator = INSTANCE_LOCATOR,
    userId = userName,
    dependencies = TestChatkitDependencies(
        tokenProvider = TestTokenProvider(INSTANCE_ID, userName, AUTH_KEY_ID, AUTH_KEY_SECRET)
    )
).also { managers += it }

fun closeChatManagers() {
    managers.forEach { it.close() }
    managers.clear()
}

fun <A> Result<A, elements.Error>.assumeSuccess(): A = when (this) {
    is Result.Success -> value
    is Result.Failure -> error("Failure: $error")
}
