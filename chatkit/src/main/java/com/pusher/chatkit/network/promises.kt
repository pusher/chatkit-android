package com.pusher.chatkit.network

import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.Result
import com.pusher.util.flatMapResult
import com.pusher.util.orElse
import com.pusher.platform.network.OkHttpResponsePromise
import elements.Error

internal inline fun <reified A> OkHttpResponsePromise.parseResponseWhenReady(): Promise<Result<A, Error>> =
    flatMapResult { response ->
        response.body()
            .orElse { elements.Errors.other("No body in response: $response") }
            .flatMap { it.charStream().parseAs<A>() }
            .asPromise()
    }
