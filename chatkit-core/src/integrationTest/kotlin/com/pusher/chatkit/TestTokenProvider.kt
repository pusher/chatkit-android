package com.pusher.chatkit

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC256
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import java.util.*
import java.util.concurrent.Future

data class TestTokenProvider(
    private val instanceId: String,
    private val userId: String,
    private val keyId: String,
    private val secret: String,
    private val su: Boolean = false
) : TokenProvider {
    override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> = Futures.now(
        JWT.create()
            .withClaim("instance", instanceId)
            .withClaim("iss", "api_keys/$keyId")
            .withClaim("iat", Date())
            .withClaim("exp", Date(Date().time + 3_600_000))
            .withClaim("sub", userId)
            .withClaim("su", su)
            .sign(HMAC256(secret))
            .asSuccess()
    )

    override fun clearToken(token: String?) = Unit


}
