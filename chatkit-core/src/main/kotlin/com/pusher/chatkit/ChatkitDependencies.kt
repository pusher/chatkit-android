package com.pusher.chatkit

import com.pusher.SdkInfo
import com.pusher.platform.MediaTypeResolver
import com.pusher.platform.PlatformDependencies
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import java.io.File

open class ChatkitDependencies(
    val tokenProvider: TokenProvider,
    override val logger: Logger = EmptyLogger(),
    override val mediaTypeResolver: MediaTypeResolver = NullMediaTypeResolver(),
    override val sdkInfo: SdkInfo = SdkInfo(
        product = "Chatkit",
        sdkVersion = SDK_VERSION,
        platform = "JVM",
        language = "Kotlin"
    )
) : PlatformDependencies

open class EmptyLogger : Logger {
    override fun debug(message: String, error: Error?) { }
    override fun error(message: String, error: Error?) { }
    override fun info(message: String, error: Error?) { }
    override fun verbose(message: String, error: Error?) { }
    override fun warn(message: String, error: Error?) { }
}

class NullMediaTypeResolver : MediaTypeResolver {
    override fun fileMediaType(file: File): String? = null
}

const val SDK_VERSION = "2.0.0-alpha1" // should match VERSION_NAME in parent's gradle.properties
