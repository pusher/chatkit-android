package com.pusher.chatkit

import com.pusher.CoreBuildConfig
import com.pusher.SdkInfo
import com.pusher.platform.MediaTypeResolver
import com.pusher.platform.PlatformDependencies
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import java.io.File

/**
 * Dependencies used to configure the Chatkit SDK, primarily to set your
 * [TokenProvider] and optionally set other dependencies like your [Logger].
 *
 * @param tokenProvider for providing your JWT auth tokens, depending on your backend you may
 * consider using the exiting [ChatkitTokenProvider] implementation
 * @param logger pass if you want the SDK to log messages
 * @param mediaTypeResolver pass if you upload files for message attachments so that
 * HTTP Content-Type is set
 * @param sdkInfo consider passing if you are building a specialised JVM-based SDK
 */
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

/**
 * No-operation implementation. Extend if you just want to log messages only at specific levels.
 */
open class EmptyLogger : Logger {
    override fun debug(message: String, error: Error?) { /* nop */ }
    override fun error(message: String, error: Error?) { /* nop */ }
    override fun info(message: String, error: Error?) { /* nop */ }
    override fun verbose(message: String, error: Error?) { /* nop */ }
    override fun warn(message: String, error: Error?) { /* nop */ }
}

/**
 * Implementation that resolves file media types as unknown (`null`).
 */
class NullMediaTypeResolver : MediaTypeResolver {
    override fun fileMediaType(file: File): String? = null
}

internal const val SDK_VERSION = CoreBuildConfig.VERSION
