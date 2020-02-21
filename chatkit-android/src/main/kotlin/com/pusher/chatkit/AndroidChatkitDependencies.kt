package com.pusher.chatkit

import android.content.Context
import com.pusher.platform.AndroidMediaTypeResolver
import com.pusher.platform.MediaTypeResolver
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider

class AndroidChatkitDependencies @JvmOverloads constructor(
    val context: Context,
    tokenProvider: TokenProvider,
    override val logger: Logger = EmptyLogger(),
    override val mediaTypeResolver: MediaTypeResolver = AndroidMediaTypeResolver()
) : ChatkitDependencies(tokenProvider) {

    override val sdkInfo = super.sdkInfo.copy(platform = "Android")
}
