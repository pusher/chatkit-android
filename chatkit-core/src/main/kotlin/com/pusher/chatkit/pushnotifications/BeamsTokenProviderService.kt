package com.pusher.chatkit.pushnotifications

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.util.Result

data class BeamsTokenProviderResponse(val token: String?)

class BeamsTokenProviderService(
        private val beamsTokenProviderClient: PlatformClient
) {
  fun fetchToken(userId: String): Result<BeamsTokenProviderResponse, elements.Error> {
    return beamsTokenProviderClient.doRequest<BeamsTokenProviderResponse>(
            options = RequestOptions(
                    path = "/beams-tokens?user_id=$userId",
                    method = "GET"
            ),
            responseParser = { it.parseAs() }
    )
  }
}
