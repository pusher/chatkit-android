package com.pusher.chatkit

import com.pusher.platform.Instance
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import java.util.concurrent.Future

internal fun chatForFunctionalTest(
    userId: String,
    platformClientFactory: PlatformClientFactory = DefaultPlatformClientFactory()
) =
        SynchronousChatManager(
                "dummyVersion:dummyCluster:dummyInstanceId",
                userId,
                TestChatkitDependencies(DummyTokenProvider()),
                platformClientFactory
        )

class DummyTokenProvider : TokenProvider {

    override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> =
        Futures.now("dummyToken".asSuccess())

    override fun clearToken(token: String?) { /* nop */ }
}

internal fun testPlatformClientFactory(mockPlatformClient: PlatformClient) =
        object : PlatformClientFactory {

            override fun createPlatformClient(
                instance: Instance,
                tokenProvider: TokenProvider
            ): PlatformClient {

                return mockPlatformClient
            }
        }
