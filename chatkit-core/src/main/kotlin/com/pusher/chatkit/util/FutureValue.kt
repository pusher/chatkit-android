package com.pusher.chatkit.util

import com.pusher.platform.network.Futures
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KProperty

class FutureValue<A>(private val wait: Wait = Wait.For(10, TimeUnit.SECONDS)) {
    private val queue: BlockingQueue<Value<A>> = SynchronousQueue(true)
    private val future = Futures.schedule { queue.take().value }
    private val singleSetGuard = AtomicBoolean()
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): A = get()
    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: A) = set(value)
    fun get(): A = future.wait(wait)
    fun set(value: A) {
        if (singleSetGuard.getAndSet(true)) {
            error("FutureValue supports setting value only once, " +
                    "existing: ${queue.take().value}, attempted: $value")
        }
        queue.put(Value(value))
    }
}

/**
 * [SynchronousQueue] doesn't take nullable objects so have to wrap it on this.
 */
private data class Value<A>(val value: A)
