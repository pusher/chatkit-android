package com.pusher.chatkit

import com.pusher.SdkInfo
import com.pusher.platform.MediaTypeResolver
import com.pusher.platform.PlatformDependencies
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun createTestChatkitDependencies(tokenProvider: TokenProvider): ChatkitDependencies {
    val testPlatformDependencies = TestPlatformDependencies()
    return ChatkitDependencies(
        tokenProvider,
        testPlatformDependencies.logger,
        testPlatformDependencies.mediaTypeResolver,
        testPlatformDependencies.sdkInfo
    )
}

class TestPlatformDependencies : PlatformDependencies {
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
        product = "Chatkit Tests",
        sdkVersion = SDK_VERSION,
        platform = "JVM",
        language = "Kotlin"
    )
}

private const val MAX_LOG_LENGTH = 3000
