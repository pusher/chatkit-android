package com.pusher.chatkit.cursors

import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.platform.network.map
import com.pusher.platform.network.wait
import com.pusher.util.Result
import com.pusher.util.asFailure
import elements.Error
import elements.Errors
import java.lang.Thread.*
import java.util.concurrent.Future
import kotlin.properties.Delegates

/**
 * Used to do a trailing throttle of actions. The [currentTask] is cancelled if a new action comes in before.
 */
internal class Throttler<A> {

    private var currentTask by Delegates.observable<Task<A>?>(null) { _, old, _ ->
        old?.cancel()
    }

    fun throttle(future: Future<Result<A, Error>>, delay: Long = 500): Future<Result<A, Error>> =
        Futures.schedule { currentTask = Task(future) }
            .map { sleep(delay) }
            .map { currentTask?.invoke().also { currentTask = null }  }
            .map { result -> result ?: fail("No current task") }

    class Task<A>(val future: Future<Result<A, Error>>) : () -> Result<A, Error> {

        override fun invoke(): Result<A, Error> =
            if (future.isCancelled) fail("Skipped request") else future.wait()

        fun cancel() = future.cancel()

    }

}

private fun <A> fail(reason: String) = Errors.other(reason).asFailure<A, Error>()
