package com.pusher.chatkit

import android.content.Context
import com.pusher.SdkInfo
import com.pusher.platform.AndroidDependencies
import com.pusher.platform.PlatformDependencies
import com.pusher.platform.tokenProvider.TokenProvider
import okhttp3.OkHttpClient

/**
 * [ChatkitDependencies] implementation for Android.
 */
data class AndroidChatkitDependencies constructor(
    override val tokenProvider: TokenProvider,
    override val tokenParams: ChatkitTokenParams?,
    override val okHttpClient: OkHttpClient?,
    private val platformDependencies: PlatformDependencies
) : ChatkitDependencies, PlatformDependencies by platformDependencies {

    @JvmOverloads
    constructor(
        context: Context,
        tokenProvider: TokenProvider,
        tokenParams: ChatkitTokenParams? = null,
        okHttpClient: OkHttpClient? = null
    ) : this(tokenProvider, tokenParams, okHttpClient, context.androidChatkitDependencies)

}

private val Context.androidChatkitDependencies
    get() = AndroidDependencies(applicationContext, SdkInfo(
        product = "Chatkit",
        sdkVersion = BuildConfig.VERSION_NAME,
        platform = "Android",
        language = "Kotlin/Java"
    ))