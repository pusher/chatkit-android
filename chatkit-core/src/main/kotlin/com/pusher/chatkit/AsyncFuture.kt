package com.pusher.chatkit

import com.pusher.platform.network.Promise
import java.util.concurrent.*
import kotlin.reflect.KProperty

private val executor = Executors.newSingleThreadExecutor()

/**
 * Similar to [java.util.concurrent.CompletableFuture] but supported in older version of Android.
 */
class AsyncFuture<V> private constructor(
    private val continuation: Continuation<V>
) : Future<V> by continuation.concrete {

    constructor(run: FutureContinuation<V>.() -> Unit) : this(Continuation()) {
        executor.submit(continuation.concrete)
        continuation.run()
    }

}

/**
 * Internal implementation of [FutureContinuation]
 */
private class Continuation<V> : FutureContinuation<V> {

    private val queue: BlockingQueue<Token<V>> = SynchronousQueue<Token<V>>()
    val concrete: FutureTask<V> = FutureTask { queue.take().value }

    override fun complete(value: V) = when {
        concrete.isDone -> error("Future is already finished")
        else -> queue.put(Token(value))
    }

    override val isComplete: Boolean
        get() = concrete.isDone

}

/**
 * Used to represent a value, since [SynchronousQueue] doesn't support nulls but V could be nullable
 */
private data class Token<V>(val value: V)

/**
 * Used to complete a [AsyncFuture]
 */
interface FutureContinuation<V> {

    /**
     * Whether the future is completed or not
     */
    val isComplete: Boolean

    /**
     * It will signal the future that we now have a value or fail if it has already been completed.
     */
    fun complete(value: V)

}

fun <V> Promise<V>.toFuture() : Future<V> =
    AsyncFuture { onReady { complete(it) } }

/**
 * Adds the option to use a [Future] as a blocking delegated property.
 */
operator fun <V> Future<V>.getValue(thisRef: Nothing?, property: KProperty<Any?>): V = get()
