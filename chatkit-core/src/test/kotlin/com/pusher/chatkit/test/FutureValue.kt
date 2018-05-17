package com.pusher.chatkit.test

import com.pusher.platform.network.Futures
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty

class FutureValue<A>(private val wait: Wait = Wait.For(10, TimeUnit.SECONDS)) {
    private val queue: BlockingQueue<Value<A>> = SynchronousQueue(true)
    private val future = Futures.schedule { queue.take().value }
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): A = get()
    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: A) = set(value)
    fun get(): A = future.wait(wait)
    fun set(value: A) = queue.put(Value(value))
}

/**
 * [SynchronousQueue] doesn't take nullable objects so have to wrap it on this.
 */
private data class Value<A>(val value: A)

