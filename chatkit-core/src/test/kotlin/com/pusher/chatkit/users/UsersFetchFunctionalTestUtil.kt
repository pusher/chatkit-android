package com.pusher.chatkit.users

import com.nhaarman.mockitokotlin2.*
import com.pusher.chatkit.PlatformClient
import com.pusher.util.asFailure
import elements.Error
import org.mockito.ArgumentMatchers.startsWith

internal fun usersFetchFailingWith(error: Error)
        : KStubbing<PlatformClient>.(PlatformClient) -> Unit = { client ->
    on {
        client.doRequest<List<User>>(
                method = eq("GET"),
                path = startsWith("/users_by_ids"),
                body = anyOrNull(),
                responseParser = any()
        )
    } doReturn error.asFailure()
}