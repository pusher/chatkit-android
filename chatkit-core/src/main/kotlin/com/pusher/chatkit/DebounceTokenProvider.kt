package com.pusher.chatkit

import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.Error
import java.util.concurrent.Future

/**
 * Used to avoid multiple requests to the tokenProvider if one is pending
 */
internal class DebounceTokenProvider(private val original: TokenProvider) : TokenProvider {

    private var pending: Future<Result<String, Error>>? = null

    override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> = synchronized(this) {
        val initialPendingSnapshot = pending
        if (initialPendingSnapshot != null &&
                (initialPendingSnapshot.isDone || initialPendingSnapshot.isCancelled)) {

            pending = null
        }

        pending ?: original.fetchToken(tokenParams).also { pending = it }
    }

    override fun clearToken(token: String?) = synchronized(this) {
        original.clearToken(token)
        pending = null
    }
}
