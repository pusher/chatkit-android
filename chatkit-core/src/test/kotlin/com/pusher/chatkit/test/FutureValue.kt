package com.pusher.chatkit.test

import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import kotlin.reflect.KProperty

class FutureValue<A> {
    private val queue: BlockingQueue<A> = SynchronousQueue<A>(true)
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): A = queue.take()
    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: A) = queue.put(value)
}