package com.pusher.chatkit

import com.google.common.truth.Truth.*
import com.pusher.SdkInfo
import mokitox.stub
import org.junit.jupiter.api.Test

internal class AndroidChatkitDependenciesTest {

    @Test
    fun `sdkInfo is injected`() {
        val dependencies = AndroidChatkitDependencies(
            tokenProvider = stub(),
            okHttpClient = stub(),
            logger = stub(),
            platformDependencies = stub()
        )

        assertThat(dependencies.sdkInfo).isEqualTo(SdkInfo(
            product = "Chatkit",
            sdkVersion = BuildConfig.VERSION_NAME,
            platform = "Android",
            language = "Kotlin/Java"
        ))
    }

}
