package com.pusher.chatkit.cursors

import com.pusher.platform.network.Futures
import java.lang.Thread.sleep
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

/**
 * Used to do a trailing throttle of actions.
 */
internal class Throttler<A, B>(
    private val delay: Long = 500,
    private val action: (A) -> B
) {

    private val target = AtomicReference<A>()

    var future: Future<B>? = null

    fun throttle(input: A): Future<B> {
        target.set(input)
        return future?.takeUnless { it.isDone } ?: schedule()
    }

    private fun schedule() = Futures.schedule {
        sleep(delay)
        action(target.get())
    }.also { future = it }

}
