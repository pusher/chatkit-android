package com.pusher.chatkit

import com.pusher.chatkit.util.parseAs
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.flatten
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.Date
import java.util.concurrent.Future

// TODO: needs more refinements, essentially userId, authData -> queryParameters, headers
//  + creation of TestChatkitTokenProvider (accepting userId) that will use it
/**
 * Simple [TokenProvider] implementation which uses `POST` request method
 * passing [userId] as a query parameter and [authData] within form-encoded request body
 * and caches a fetched token in memory.
 *
 * @param endpoint url of the token providing endpoint
 * @param authData provider of auth data to be placed in the request's form-encoded body
 * @param client optional custom instance of [OkHttpClient]
 * @param tokenCache optional custom implementation of [TokenCache]
 */
class ChatkitTokenProvider
@JvmOverloads constructor(
    private val endpoint: String,
    private val userId: String,
    private val authData: () -> Map<String, String>,
    private val client: OkHttpClient = OkHttpClient(),
    private val tokenCache: TokenCache = InMemoryTokenCache(Clock())
) : TokenProvider {

    private val httpUrl =
        HttpUrl.parse(endpoint)
            ?.newBuilder()
            ?.apply { addQueryParameter("user_id", userId) }
            ?.build()
            ?: throw IllegalArgumentException("Token Provider endpoint is not valid URL")

    /**
     * @param tokenParams this implementation does not use [tokenParams], it uses [authData] instead
     */
    override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> =
        when (val cachedToken = tokenCache.getTokenFromCache()) {
            null -> fetchTokenFromEndpoint()
            else -> Futures.now(cachedToken.asSuccess())
        }

    override fun clearToken(token: String?) {
        tokenCache.clearCache()
    }

    private fun fetchTokenFromEndpoint(): Future<Result<String, Error>> =
        Futures.schedule {
            val request = Request.Builder()
                .url(httpUrl)
                .post(requestBody())
                .build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful && response.code() in 200..299) {
                parseTokenResponse(response)
            } else {
                response.asError().asFailure()
            }
        }

    private fun requestBody() = FormBody.Builder().apply {
        add("grant_type", "client_credentials")
        add(authData())
    }.build()

    private fun FormBody.Builder.add(map: Map<String, String>) {
        for ((k, v) in map) {
            add(k, v)
        }
    }

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

private data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: String,
    val refreshToken: String
)

/**
 * Default token expiry tolerance — 10 minutes.
 */
const val CACHE_EXPIRY_TOLERANCE = 10 * 60

/**
 * Interface for token cached used by [ChatkitTokenProvider].
 */
interface TokenCache {

    /**
     * Store the passed token in the cache.
     *
     * @param token the token value to store
     * @param expiresIn seconds until token expiry
     */
    fun cache(token: String, expiresIn: Long)

    /**
     * Get the currently cached token, if any.
     *
     * @return the token that is currently cached (if not expired),
     * or null if there is no token (or the token is expired)
     */
    fun getTokenFromCache(): String?

    /**
     * Clear the currently stored token from the cache.
     */
    fun clearCache()
}

/**
 * A simple in-memory token cache implementation with expiry tolerance of 10 minutes.
 */
internal class InMemoryTokenCache(private val clock: Clock) : TokenCache {
    private var token: String? = null
    private var expiration: Long = -1

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
 * Class for providing the current time.
 */
internal class Clock {

    /**
     * @return seconds since the Epoch
     */
    fun currentTimestampInSeconds(): Long {
        return Date().time / 1000
    }
}
