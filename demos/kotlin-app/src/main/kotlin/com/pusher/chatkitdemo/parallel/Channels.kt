package com.pusher.chatkitdemo.parallel

import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch


fun <A> lazyBroadcast(block: suspend BroadcastChannel<A>.() -> Unit = {}) =
    lazy { broadcast(block) }

fun <A> broadcast(block: suspend BroadcastChannel<A>.() -> Unit = {}) =
    BroadcastChannel<A>(Channel.CONFLATED).also {
        launch { block(it) }
    }
