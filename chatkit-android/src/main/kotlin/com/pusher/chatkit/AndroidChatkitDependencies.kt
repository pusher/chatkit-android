package com.pusher.chatkit

import android.content.Context
import com.pusher.platform.AndroidMediaTypeResolver
import com.pusher.platform.MediaTypeResolver
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider

/**
 * Dependencies used to configure the Chatkit SDK, primarily to set your Android [Context] and
 * [TokenProvider], as well as optionally other dependencies like your [Logger].
 *
 * @param context Android context used for string formatting in ViewModels respecting
 * device settings and for push notifications (usually [android.app.Application] but if you use
 * the SDK just in a shorter-lived scope like [android.app.Activity] you can pass it as well)
 * @param tokenProvider for providing your JWT auth tokens, depending on your backend you may
 * consider using the existing [ChatkitTokenProvider] implementation
 * @param logger pass if you want the SDK to log messages
 * @param mediaTypeResolver pass if you upload files for message attachments so that
 * HTTP Content-Type is set
 */
class AndroidChatkitDependencies @JvmOverloads constructor(
    val context: Context,
    tokenProvider: TokenProvider,
    override val logger: Logger = EmptyLogger(),
    override val mediaTypeResolver: MediaTypeResolver = AndroidMediaTypeResolver()
) : ChatkitDependencies(tokenProvider) {

    override val sdkInfo = super.sdkInfo.copy(platform = "Android")
}
