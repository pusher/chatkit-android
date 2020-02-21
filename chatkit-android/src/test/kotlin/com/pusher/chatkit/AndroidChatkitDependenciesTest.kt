package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.SdkInfo
import mockitox.stub
import org.junit.jupiter.api.Test

internal class AndroidChatkitDependenciesTest {

    @Test
    fun `sdkInfo is injected`() {
        val dependencies = AndroidChatkitDependencies(
                context = stub(),
                tokenProvider = stub()
        )

        assertThat(dependencies.sdkInfo).isEqualTo(SdkInfo(
                product = "Chatkit",
                sdkVersion = "2.0.0-alpha1",
                platform = "Android",
                language = "Kotlin"
        ))
    }
}
