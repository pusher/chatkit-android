package com.pusher.chatkit.pushnotifications

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import java.net.URLEncoder

data class BeamsTokenProviderResponse(val token: String?)

class BeamsTokenProviderService(
        private val beamsTokenProviderClient: PlatformClient
) {
    fun fetchToken(userId: String): Result<BeamsTokenProviderResponse, elements.Error> {
        return beamsTokenProviderClient.doRequest(
                options = RequestOptions(
                        path = "/beams-tokens?user_id=${URLEncoder.encode(userId, "UTF-8")}",
                        method = "GET"
                ),
                responseParser = { it.parseAs<BeamsTokenProviderResponse>() }
        )
    }
}