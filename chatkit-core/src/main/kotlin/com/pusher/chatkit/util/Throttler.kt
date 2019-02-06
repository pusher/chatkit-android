package com.pusher.chatkit.util

import com.pusher.platform.network.Futures
import java.lang.Thread.sleep
import java.util.concurrent.Future

/**
 * Used to do a trailing throttle of actions.
 */
internal class Throttler<A, B>(
        private val delay: Long = 500,
        private val action: (A) -> B
) {
    private val updateLock = object {}
    private var target: A? = null
    private var future: Future<B>? = null

    fun throttle(input: A): Future<B> {
        synchronized(updateLock) {
            target = input
            if (future == null) future = schedule()

            return future!!
        }
    }

    private fun schedule() = Futures.schedule {
        sleep(delay)
        val targetNow =
                synchronized(updateLock) {
                    future = null
                    target
                }
        action(targetNow!!)
    }
}
