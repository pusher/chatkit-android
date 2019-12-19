package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.ChatEvent.CurrentUserReceived
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.util.FutureValue
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.Error
import elements.Errors
import elements.emptyHeaders
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

object ChatManagerWithFailingTokenProviderSpec : Spek({
    afterEachTest(::closeChatManagers)
    afterEachTest(::tearDownInstance)

    val futureConnectionResult by memoized { FutureValue<Result<CurrentUser, Error>>() }
    val notifiedEvents by memoized { ConcurrentLinkedQueue<ChatEvent>() }

    describe("given token provider fails with 401") {
        val tokenProviderError = Errors.response(401, emptyHeaders(), "auth expired")

        val subject by memoized {
            createChatManager(tokenProvider = object : TokenProvider {

                override fun fetchToken(tokenParams: Any?)
                        : Future<Result<String, Error>> =
                        Futures.schedule {
                            sleep(200) // more realistic with that delay
                            Result.failure<String, Error>(tokenProviderError)
                        }

                override fun clearToken(token: String?) { /* nop */ }

            })
        }

        describe("when connect is called") {
            beforeEachTest {
                subject.connect(
                        consumer = { event -> notifiedEvents.add(event) },
                        callback = { result -> futureConnectionResult.set(result) }
                )
            }

            it("then the connection result indicates and contains the correct error") {
                assertThat(futureConnectionResult.get()).apply {
                    isInstanceOf(Result.Failure::class.java)
                    isEqualTo(Result.failure<String, Error>(tokenProviderError))
                }
            }
            it("then no events are notified") {
                futureConnectionResult.get() // wait to connect
                assertThat(notifiedEvents).isEmpty()
            }
        }
    }

    describe("given token provider fails twice with a network error and then succeeds") {
        beforeEachTest { setUpInstanceWith(createDefaultRole(), newUsers(Users.PUSHERINO)) }

        val subject by memoized {
            val testTokenProvider = TestTokenProvider(INSTANCE_ID, Users.PUSHERINO,
                    AUTH_KEY_ID, AUTH_KEY_SECRET)

            val tokenProviderError = Errors.network("connectivity issue")

            createChatManager(tokenProvider = object : TokenProvider by testTokenProvider {

                var errorCounter = AtomicInteger(2)

                override fun fetchToken(tokenParams: Any?)
                        : Future<Result<String, Error>> =
                        if (errorCounter.getAndDecrement() > 0) {
                            Futures.schedule {
                                sleep(10) // more realistic with that tiny delay
                                Result.failure<String, Error>(tokenProviderError)
                            }
                        } else {
                            testTokenProvider.fetchToken(tokenParams)
                        }

            })
        }

        describe("when connect is called") {
            beforeEachTest {
                subject.connect(
                        consumer = { event -> notifiedEvents.add(event) },
                        callback = { result -> futureConnectionResult.set(result) }
                )
            }

            it("then the connection result indicates success") {
                assertThat(futureConnectionResult.get()).isInstanceOf(Result.Success::class.java)
            }
            it("then only the current user received event is notified") {
                futureConnectionResult.get() // wait to connect
                assertThat(notifiedEvents.size).isEqualTo(1)
                assertThat(notifiedEvents.peek()).isInstanceOf(CurrentUserReceived::class.java)
            }
        }
    }

})