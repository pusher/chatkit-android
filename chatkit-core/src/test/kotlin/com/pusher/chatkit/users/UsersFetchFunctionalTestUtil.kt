package com.pusher.chatkit.users

import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.users.api.UserApiType
import com.pusher.util.asFailure
import elements.Error
import org.mockito.ArgumentMatchers.startsWith

internal fun usersFetchFailingWith(error: Error):
        KStubbing<PlatformClient>.(PlatformClient) -> Unit = { client ->
    on {
        client.doRequest<List<UserApiType>>(
                method = eq("GET"),
                path = startsWith("/users_by_ids"),
                body = anyOrNull(),
                responseParser = any()
        )
    } doReturn error.asFailure()
}
