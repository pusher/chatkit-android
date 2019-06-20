package com.pusher.chatkit.pushnotifications

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.util.orElse
import elements.Errors
import java.net.URLEncoder

data class BeamsTokenProviderResponse(val token: String?)

class BeamsTokenProviderService(
        private val beamsTokenProviderClient: PlatformClient
) {
    fun fetchToken(userId: String): String =
            beamsTokenProviderClient.doRequest(
                    options = RequestOptions(
                            path = "/beams-tokens?user_id=${URLEncoder.encode(userId, "UTF-8")}",
                            method = "GET"
                    ),
                    responseParser = { it.parseAs<BeamsTokenProviderResponse>() }
            ).mapFailure {
                Errors.other("Could not authenticate with push notifications service: $it")
            }.flatMap { response ->
                response.token.orElse {
                    Errors.other("Could not get auth token for push notification service")
                }
            }.successOrThrow()
}
