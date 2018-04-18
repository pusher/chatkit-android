package com.pusher.chatkit

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC256
import com.pusher.platform.network.Promise
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import java.util.*

class TestTokenProvider(
    private val instanceId: String,
    private val userId: String,
    private val keyId: String = "6b47021e-baad-4380-bc9f-5ca43c0dff2a",
    private val secret: String = "JALixNRZLgLeFDowYLRhGeuwJD8xCR8zUOC3YtV9eJI=",
    private val su: Boolean = false
) : TokenProvider {
    override fun fetchToken(tokenParams: Any?): Promise<Result<String, Error>> = Promise.now(
        JWT.create()
            .withClaim("instance", instanceId)
            .withClaim("iss", "api_keys/$keyId")
            .withClaim("iat", Date())
            .withClaim("exp", Date(Date().time + 3_600))
            .withClaim("sub", userId)
            .withClaim("su", su)
            .sign(HMAC256(secret))
            .asSuccess()
    )

    override fun clearToken(token: String?) = Unit


}