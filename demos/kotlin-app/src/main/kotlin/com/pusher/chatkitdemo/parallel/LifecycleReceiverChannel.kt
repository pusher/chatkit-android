package com.pusher.chatkitdemo.parallel

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Lifecycle.Event.*
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.toChannel
import kotlinx.coroutines.experimental.launch

fun <A> LifecycleOwner.onLifecycle(block: () -> ReceiveChannel<A>): ReceiveChannel<A> =
    LifecycleReceiverChannel(this.lifecycle, block)

private class LifecycleReceiverChannel<out A>(
    lifecycle: Lifecycle,
    private val block: () -> ReceiveChannel<A>,
    private val broadcastChannel: BroadcastChannel<A> = BroadcastChannel(Channel.CONFLATED),
    private val subscription: ReceiveChannel<A> = broadcastChannel.openSubscription()
) : ReceiveChannel<A> by subscription, LifecycleObserver {

    private var channel: ReceiveChannel<A>? = null

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        channel?.cancel()
        channel = block()
        launch { channel?.toChannel(broadcastChannel) }
    }

    @OnLifecycleEvent(ON_STOP)
    fun onStop() {
        channel?.cancel()
    }

    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy() {
        subscription.cancel()
    }

}
