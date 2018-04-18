package com.pusher.chatkit.test

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import org.jetbrains.spek.api.dsl.TestBody
import org.jetbrains.spek.api.dsl.TestContainer
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS


/**
 * Spek extension to allow a test to block for threaded completion. It can be used inside a
 * [TestContainer] by using the function `will("do something") {}`.
 *
 * To suspend the test we need to call `await()`.
 *
 * To finish the suspension we can either call `done()`, `done {}` with an action (i.e. fail test)
 * or `fail(cause)`.
 *
 */
class SuspendedTestBody(body: SuspendedTestBody.() -> Unit) : TestBody {

    private val completeChannel = Channel<() -> Unit>()

    init {
        launch {
            body()
        }
    }

    /**
     * signals the test to stop with an action that could fail. This will run on the original thread.
     */
    fun done(action: () -> Unit = {}) =
        completeChannel.offer(action)

    /**
     * Signals the test to fail with the provided [cause]
     */
    fun fail(cause: String) = fail(Error(cause))

    /**
     * Signals the test to fail with the provided [cause]
     */
    fun fail(cause: Throwable) = done { throw cause }

    /**
     * Tries to run the provided [action] failing the test if it throws an exception.
     */
    fun attempt(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            fail(e)
        }
    }

    /**
     * Waits for [done], [fail] or [attempt] with failure are called.
     */
    suspend fun await(timeout: Timeout = Timeout.Some(5000)) = when (timeout) {
        is Timeout.Some -> withTimeout(timeout.amount, timeout.unit) { completeChannel.receive() }
        is Timeout.None -> completeChannel.receive()
    }.also { completeChannel.close() }

}

/**
 * Describes whether and for how long [SuspendedTestBody] should wait for.
 */
sealed class Timeout {
    data class Some(val amount: Long, val unit: TimeUnit = MILLISECONDS) : Timeout()
    /**
     * Only useful for debugging, not to be used in live tests.
     */
    object None : Timeout()
}

/**
 * Main entry point for [SuspendedTestBody], use this to create a test that will wait for async termination or [timeout]
 */
fun TestContainer.will(
    description: String,
    timeout: Timeout = Timeout.Some(5000),
    body: SuspendedTestBody.() -> Unit
): Unit = it("will $description") {
    runBlocking { SuspendedTestBody(body).await(timeout) }.invoke()
}
