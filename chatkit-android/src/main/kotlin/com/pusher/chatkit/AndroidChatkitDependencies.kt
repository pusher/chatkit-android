package com.pusher.chatkit

import com.pusher.SdkInfo
import com.pusher.platform.AndroidDependencies
import com.pusher.platform.MediaTypeResolver
import com.pusher.platform.PlatformDependencies
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import okhttp3.OkHttpClient

/**
 * [ChatkitDependencies] implementation for Android using [AndroidDependencies] to fulfil [PlatformDependencies].
 */
data class AndroidChatkitDependencies @JvmOverloads constructor(
    override val tokenProvider: TokenProvider,
    override val tokenParams: ChatkitTokenParams? = null,
    override val okHttpClient: OkHttpClient? = null,
    private val platformDependencies: PlatformDependencies = AndroidDependencies(),
    override val logger: Logger = platformDependencies.logger
) : ChatkitDependencies {

    override val mediaTypeResolver: MediaTypeResolver = platformDependencies.mediaTypeResolver
    override val sdkInfo: SdkInfo = chatkitSdkInfo

}

private val chatkitSdkInfo get() = SdkInfo(
    product = "Chatkit",
    sdkVersion = BuildConfig.VERSION_NAME,
    platform = "Android",
    language = "Kotlin/Java"
)
