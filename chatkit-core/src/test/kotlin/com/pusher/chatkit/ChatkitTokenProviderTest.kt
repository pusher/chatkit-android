package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.platform.network.wait
import com.pusher.util.Result
import com.pusher.util.asFailure
import elements.Error
import elements.Errors
import java.util.Date
import mockitox.mock
import mockitox.returns
import mockitox.returnsStub
import mockitox.stub
import okhttp3.Call
import okhttp3.Headers
import okhttp3.RequestBody
import okio.Buffer
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.notNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

private val testProvider = ChatkitTokenProvider(
        endpoint = "https://localhost",
        userId = "pusherino"
)

internal class ChatkitTokenProviderTest : Spek({

    describe("ChatkitTokenProvider") {

        it("provides token") {
            val provider = testProvider.copy(
                    client = mock {
                        newCall(argThat {
                            it.url().toString() == "https://localhost/?user_id=pusherino" &&
                                    it.body().toBuffer() == "grant_type=client_credentials".toBuffer()
                        }) returnsStub withSuccess()
                    }
            )

            val token = provider.fetchToken(null).wait().assumeSuccess()

            assertThat(token).isEqualTo("goodToken")
        }

        it("provides ChatkitTokenParams as part of request") {
            val provider = testProvider.copy(
                    client = mock {
                        newCall(argThat {
                            it.url().toString() == "https://localhost/?user_id=pusherino" &&
                                    it.body().toBuffer() == "grant_type=client_credentials&key=value".toBuffer()
                        }) returnsStub withSuccess()
                    }
            )

            val token = provider.fetchToken(ChatkitTokenParams(
                    mapOf("key" to "value")
            )).wait().assumeSuccess()

            assertThat(token).isEqualTo("goodToken")
        }

        it("provides CustomData as part of request") {
            val provider = testProvider.copy(
                    authData = mapOf("key" to "value"),
                    client = mock {
                        newCall(argThat {
                            it.url().toString() == "https://localhost/?user_id=pusherino" &&
                                    it.body().toBuffer() == "grant_type=client_credentials&key=value".toBuffer()
                        }) returnsStub withSuccess()
                    }
            )

            val token = provider.fetchToken(null).wait().assumeSuccess()

            assertThat(token).isEqualTo("goodToken")
        }

        it("provides an error when the request fails") {
            val provider = testProvider.copy(
                    client = mock {
                        newCall(notNull()) returnsStub withFailure()
                    },
                    tokenCache = stub()
            )

            val result = provider.fetchToken(null).wait()

            assertThat(result).isEqualTo(badResponse)
        }

        it("provides an error when the request is successful but not 2XX.") {
            val provider = testProvider.copy(
                    client = mock {
                        newCall(notNull()) returnsStub withNotOkResponse()
                    },
                    tokenCache = stub()
            )

            val result = provider.fetchToken(null).wait()

            assertThat(result).isEqualTo(notOkResponse)
        }

        it("gets token from cache") {
            val provider = testProvider.copy(
                    client = mock {
                        newCall(notNull()) returnsStub withSuccess()
                        newCall(notNull()) returnsStub withFailure() // fail on second request
                    }
            )

            val firstResult = provider.fetchToken(null).wait().assumeSuccess()
            val secondResult = provider.fetchToken(null).wait().assumeSuccess()

            assertThat(firstResult).isEqualTo(secondResult)
        }

        it("clears the cache") {
            val provider = testProvider.copy(
                    client = mock {
                        newCall(notNull()) returnsStub withSuccess()
                        newCall(notNull()) returnsStub withFailure() // fail on second request
                    }
            )

            provider.fetchToken(null).wait().assumeSuccess()
            provider.clearToken()
            val result = provider.fetchToken(null).wait()

            check(result is Result.Failure)
        }

        it("fails with incorrect url") {
            var exception: Any? = null
            try {
                testProvider.copy(endpoint = "meh")
            } catch (e: IllegalArgumentException) {
                exception = e
            }
            check(exception is IllegalArgumentException)
        }
    }
})

private fun withSuccess(): Call.() -> Unit = {
    execute() returnsStub {
        isSuccessful returns true
        headers() returns Headers.of()
        code() returns 200
        body() returnsStub {
            this!!.string() returns """
                {
                  "access_token" : "goodToken",
                  "expires_in" : ${Date().time + 5_000}
                }
            """.trimIndent()
        }
    }
}

private fun withFailure(): Call.() -> Unit = {
    execute() returnsStub {
        isSuccessful returns false
        headers() returns Headers.of()
        code() returns 500
        body() returnsStub {
            this!!.string() returns "Bad Response"
        }
    }
}

private fun withNotOkResponse(): Call.() -> Unit = {
    execute() returnsStub {
        isSuccessful returns true
        headers() returns Headers.of()
        code() returns 300
        body() returnsStub {
            this!!.string() returns "Redirect"
        }
    }
}

private fun RequestBody?.toBuffer() =
        Buffer().also { this?.writeTo(it) }

private fun String.toBuffer() =
        Buffer().also { it.writeUtf8(this) }

val badResponse = Errors.response(500, emptyMap(), "Bad Response").asFailure<String, Error>()

val notOkResponse = Errors.response(300, emptyMap(), "Redirect").asFailure<String, Error>()
