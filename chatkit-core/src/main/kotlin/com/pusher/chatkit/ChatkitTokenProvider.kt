package com.pusher.chatkit

import com.pusher.chatkit.network.parseAs
import com.pusher.platform.network.Futures
import com.pusher.platform.network.toFuture
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.Error
import elements.Errors
import okhttp3.*
import java.util.*
import java.util.concurrent.Future

typealias CustomData = Map<String, String>

/**
 * Simple token provider for Chatkit. Uses an in-memory cache for storing token.
 * @param endpoint location of this token provider.
 * @param authData data to be sent alongside each request to the token providing endpoint.
 * @param client
 * @param tokenCache
 * */
data class ChatkitTokenProvider
@JvmOverloads constructor(
    private val endpoint: String,
    internal var userId: String,
    private val authData: CustomData = emptyMap(),
    private val client: OkHttpClient = OkHttpClient(),
    private val tokenCache: TokenCache = InMemoryTokenCache(Clock())

) : TokenProvider {

    override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> {
        val cachedToken = tokenCache.getTokenFromCache()
        return when (cachedToken) {
            null -> fetchTokenFromEndpoint(tokenParams)
            else -> Futures.now(cachedToken.asSuccess())
        }
    }

    override fun clearToken(token: String?) {
        tokenCache.clearCache()
    }

    private fun fetchTokenFromEndpoint(tokenParams: Any?): Future<Result<String, Error>> =
        httpUrl.toFuture().flatMapFutureResult { url ->
            Futures.schedule {
                val request = Request.Builder().apply {
                    url(url)
                    post(requestBody(tokenParams))
                }.build()
                val response = request.let { client.newCall(it).execute() }
                when {
                    response.isSuccessful && response.code() in 200..299 -> parseTokenResponse(response)
                    else -> response.asError().asFailure()
                }
            }
        }

    private val httpUrl: Result<HttpUrl, Error>
        get() = HttpUrl.parse(endpoint)?.newBuilder()?.apply {
            addQueryParameter("user_id", userId)
        }?.build().orElse { Errors.network("Incorrect endpoint: $endpoint") }

    private fun requestBody(tokenParams: Any?) = FormBody.Builder().apply {
        add("grant_type", "client_credentials")
        add(authData)
        if (tokenParams is ChatkitTokenParams) add(tokenParams.extras)
    }.build()

    private fun FormBody.Builder.add(map: Map<String, String>) =
        map.forEach { k, v -> add(k, v) }

    private fun Response.asError(): Error = Errors.response(
        statusCode = code(),
        headers = headers().toMultimap(),
        error = body()?.string() ?: ""
    )

    private fun parseTokenResponse(response: Response): Result<String, Error> {
        return response.body()
            ?.string()
            ?.parseAs<TokenResponse>()
            .orElse { Errors.network("Could not parse token from response: $response") }
            .flatten()
            .map { token ->
                tokenCache.cache(token.accessToken, token.expiresIn.toLong())
                token.accessToken
            }
    }
}

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: String,
    val refreshToken: String
)

data class ChatkitTokenParams(
    val extras: Map<String, String> = emptyMap()
)

/**
 * Default token expiry tolerance - 10 minutes
 * */
val CACHE_EXPIRY_TOLERANCE = 10 * 60

interface TokenCache {
    /**
     * Store the current valid token in a local cache.
     * @param token the token value to store
     * @param expiresIn seconds until token expiry.
     * */
    fun cache(token: String, expiresIn: Long)


    /**
     * Get the currently cached token, if any
     * @return the token that is currently cached, if not expired, or null if there is no token, or the token is expired
     */
    fun getTokenFromCache(): String?

    /**
     * Clear the currently stored token from cache
     * */
    fun clearCache()
}

/**
 * A simple in-memory cache implementation
 * */
class InMemoryTokenCache(val clock: Clock) : TokenCache {
    var token: String? = null
    var expiration: Long = -1


    override fun cache(token: String, expiresIn: Long) {

        this.token = token
        this.expiration = clock.currentTimestampInSeconds() + expiresIn - CACHE_EXPIRY_TOLERANCE
    }

    override fun getTokenFromCache(): String? {

        val now = clock.currentTimestampInSeconds()

        return if (token != null && now < expiration) token
        else null

    }

    override fun clearCache() {
        token = null
        expiration = -1
    }
}


/**
 * Utility class we can use for mocking
 * Returns current timestamp in seconds from epoch
 * */
class Clock {
    fun currentTimestampInSeconds(): Long {
        return Date().time / 1000
    }
}
