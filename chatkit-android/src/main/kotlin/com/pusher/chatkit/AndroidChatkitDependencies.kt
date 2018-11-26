package com.pusher.chatkit

import android.content.Context
import com.pusher.SdkInfo
import com.pusher.platform.AndroidDependencies
import com.pusher.platform.PlatformDependencies
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import okhttp3.OkHttpClient

/**
 * [ChatkitDependencies] implementation for Android using [AndroidDependencies] to fulfil [PlatformDependencies].
 */
data class AndroidChatkitDependencies @JvmOverloads constructor(
        private val context: Context,
        override val tokenProvider: TokenProvider,
        override val okHttpClient: OkHttpClient? = null,
        private val platformDependencies: PlatformDependencies = AndroidDependencies(),
        override val logger: Logger = platformDependencies.logger
) : ChatkitDependencies {

    override val mediaTypeResolver = platformDependencies.mediaTypeResolver
    override val pushNotifications = BeamsPushNotificationsFactory(context)
    override val appHooks = AndroidAppHooks()

    override val sdkInfo = SdkInfo(
            product = "Chatkit",
            sdkVersion = BuildConfig.VERSION_NAME,
            platform = "Android",
            language = "Kotlin/Java"
    )
}
