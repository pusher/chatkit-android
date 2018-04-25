package com.pusher.chatkit.test

import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import kotlin.reflect.KProperty

class FutureValue<A> {
    private val queue: BlockingQueue<Value<A>> = SynchronousQueue<Value<A>>(true)
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): A = queue.take().value
    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: A) = queue.put(Value(value))
}

/**
 * [SynchronousQueue] doesn't take nullable objects so have to wrap it on this.
 */
private data class Value<A>(val value: A)

