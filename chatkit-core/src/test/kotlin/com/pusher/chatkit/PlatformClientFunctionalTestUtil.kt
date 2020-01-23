package com.pusher.chatkit

import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.mock

internal fun mockPlatformClient(
        vararg stubbings: KStubbing<PlatformClient>.(PlatformClient) -> Unit
) : PlatformClient {

    val mockPlatformClientStubbing = KStubbing(mock<PlatformClient>())

    for (stubbing in stubbings) {
        mockPlatformClientStubbing.stubbing(mockPlatformClientStubbing.mock)
    }

    return mockPlatformClientStubbing.mock
}