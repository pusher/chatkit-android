package com.pusher.chatkitdemo.parallel

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch

fun <A> LifecycleOwner.onLifecycle(block: () -> ReceiveChannel<A>): ReceiveChannel<A> =
    LifecycleReceiverChannel(this, block)

private class LifecycleReceiverChannel<out A>(
    lifecycleOwner: LifecycleOwner,
    private val block: () -> ReceiveChannel<A>,
    private val broadcastChannel: BroadcastChannel<A> = BroadcastChannel(Channel.CONFLATED),
    subscription: ReceiveChannel<A> = broadcastChannel.openSubscription()
) : ReceiveChannel<A> by subscription, LifecycleObserver {

    private var channel: ReceiveChannel<A>? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        channel?.cancel()
        channel = block()
        launch { channel?.toChannel(broadcastChannel) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        channel?.cancel()
    }

}