package com.pusher.chatkit

import com.pusher.chatkit.network.parseAs
import com.pusher.platform.network.Promise
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.Error
import elements.Errors
import okhttp3.*
import java.io.IOException
import java.util.*

/**
 * Simple token provider for Chatkit. Uses an in-memory cache for storing token.
 * @param endpoint location of this token provider.
 * @param authData data to be sent alongside each request to the token providing endpoint.
 * @param client
 * @param tokenCache
 * */
class ChatkitTokenProvider
@JvmOverloads constructor(
    private val endpoint: String,
    var userId: String,
    private val authData: CustomData = TreeMap(),
    private val client: OkHttpClient = OkHttpClient(),
    private val tokenCache: TokenCache = InMemoryTokenCache(Clock())

) : TokenProvider {

    override fun fetchToken(tokenParams: Any?): Promise<Result<String, Error>> {
        val cachedToken = tokenCache.getTokenFromCache()
        return when (cachedToken) {
            null -> fetchTokenFromEndpoint(tokenParams)
            else -> Promise.now(cachedToken.asSuccess())
        }
    }

    override fun clearToken(token: String?) {
        tokenCache.clearCache()
    }

    private fun fetchTokenFromEndpoint(tokenParams: Any?): Promise<Result<String, Error>> {
        val urlBuilder = HttpUrl.parse(endpoint)!!
            .newBuilder()
        urlBuilder.addQueryParameter("user_id", userId)

        val requestBodyBuilder = FormBody.Builder()
            .add("grant_type", "client_credentials")

        //TODO: Maybe add to query params instead???
        //Add any extras to the token provider's request.
        if (tokenParams is ChatkitTokenParams) {
            tokenParams.extras.keys.forEach { key ->
                requestBodyBuilder.add(key, tokenParams.extras.getValue(key))
            }
        }
        //TODO: Maybe add to query params instead???
        authData.keys.forEach { key ->
            requestBodyBuilder.add(key, authData.getValue(key))
        }

        val requestBody = requestBodyBuilder.build()

        val request = Request.Builder()
            .url(urlBuilder.build())
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        return Promise.promise {
            call.enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) =
                    report(Errors.network("Failed to load token: $e").asFailure())

                override fun onResponse(call: Call?, response: Response?) {
                    report(when (response?.code()) {
                        200 -> parseTokenResponse(response)
                        else -> response.asError().asFailure()
                    })
                }
            })
        }
    }

    private fun Response?.asError(): Error = when (this) {
        null -> Errors.network("No response available")
        else -> Errors.response(
            statusCode = code(),
            headers = headers().toMultimap(),
            error = body().toString()
        )
    }

    private fun parseTokenResponse(response: Response): Result<String, Error> {
        return response.body()
            ?.charStream()
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
