package com.pusher.chatkit

import com.pusher.chatkit.ChatManager.Companion.GSON
import com.pusher.platform.Cancelable
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Error
import elements.NetworkError
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
        val endpoint: String,
        var userId: String,
        val authData: CustomData = TreeMap(),
        val client: OkHttpClient = OkHttpClient(),
        val tokenCache: TokenCache = InMemoryTokenCache(Clock())

): TokenProvider {

    override fun fetchToken(tokenParams: Any?, onSuccess: (String) -> Unit, onFailure: (Error) -> Unit): Cancelable {

        val cachedToken = tokenCache.getTokenFromCache()

        return if(cachedToken != null){
            onSuccess(cachedToken)
            object: Cancelable {
                override fun cancel() {} //Nothing to cancel, we can ignore.
            }
        }
        else fetchTokenFromEndpoint(
                tokenParams = tokenParams,
                onFailure = onFailure,
                onSuccess = { token ->
                    tokenCache.cache(token.accessToken, token.expiresIn.toLong())
                    onSuccess(token.accessToken)
                })
    }

    override fun clearToken(token: String?) {
        tokenCache.clearCache()
    }


    private fun fetchTokenFromEndpoint(tokenParams: Any?, onSuccess: (TokenResponse) -> Unit, onFailure: (Error) -> Unit): Cancelable {

        var call: Call?

        val urlBuilder = HttpUrl.parse(endpoint)!!
                .newBuilder()
        urlBuilder.addQueryParameter("user_id", userId)

        val requestBodyBuilder = FormBody.Builder()
                .add("grant_type", "client_credentials")


        //TODO: Maybe add to query params instead???
        //Add any extras to the token provider's request.
        if(tokenParams is ChatkitTokenParams){
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

        call = client.newCall(request)

        call!!.enqueue( object: Callback {

            override fun onResponse(call: Call?, response: Response?) {

                if(response != null && response.code() == 200) {
                    val token = GSON.fromJson<TokenResponse>(response.body()!!.charStream(), TokenResponse::class.java)

                    tokenCache.cache(token.accessToken, token.expiresIn.toLong())
                    onSuccess(token)
                }

                else{
                    onFailure(elements.ErrorResponse(
                            statusCode = response!!.code(),
                            headers = response.headers().toMultimap(),
                            error = response.body()!!.string()
                    ))
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                onFailure(NetworkError("Failed! $e"))
            }

        })

    return object: Cancelable {
        override fun cancel() {
            call?.cancel()
        }
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
val CACHE_EXPIRY_TOLERANCE = 10*60

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
class InMemoryTokenCache(val clock: Clock): TokenCache{
    var token: String? = null
    var expiration: Long = -1


    override fun cache(token: String, expiresIn: Long){

        this.token = token
        this.expiration = clock.currentTimestampInSeconds() + expiresIn - CACHE_EXPIRY_TOLERANCE
    }

    override fun getTokenFromCache(): String? {

        val now = clock.currentTimestampInSeconds()

        return if(token != null && now < expiration) token
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
class Clock{
    fun currentTimestampInSeconds(): Long {
        return Date().time / 1000
    }
}