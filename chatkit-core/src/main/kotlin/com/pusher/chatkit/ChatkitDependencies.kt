package com.pusher.chatkit

import com.pusher.platform.PlatformDependencies
import com.pusher.platform.tokenProvider.TokenProvider

interface ChatkitDependencies : PlatformDependencies {

    val tokenProvider: TokenProvider
    val tokenParams: ChatkitTokenParams?

}