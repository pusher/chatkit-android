package com.pusher.chatkit

import com.pusher.SdkInfo
import com.pusher.chatkit.test.insecureOkHttpClient
import com.pusher.platform.*
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import okhttp3.OkHttpClient
import java.io.File

class TestDependencies : PlatformDependencies {
    override val logger: Logger = object : Logger {
        override fun verbose(message: String, error: Error?) = log("V", message, error)
        override fun debug(message: String, error: Error?) = log("D", message, error)
        override fun info(message: String, error: Error?) = log("I", message, error)
        override fun warn(message: String, error: Error?) = log("W", message, error)
        override fun error(message: String, error: Error?) = log("E", message, error)
        private fun log(type: String, message: String, error: Error?) =
            println("$type: $message ${error?.let { "\n" + it } ?: ""}")
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
    override val tokenParams: ChatkitTokenParams? = null
    override val okHttpClient: OkHttpClient = insecureOkHttpClient.newBuilder().apply {
        addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("Connection", "close").build())
        }
    }.build()
}

