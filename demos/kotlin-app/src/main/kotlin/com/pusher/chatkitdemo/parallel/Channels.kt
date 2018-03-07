package com.pusher.chatkitdemo.parallel

import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch

fun <A> broadcastToChannel(block: suspend BroadcastChannel<A>.() -> Unit = {}) =
    BroadcastChannel<A>(Channel.CONFLATED).also {
        launch { block(it) }
    }

suspend fun <A> ReceiveChannel<A>.whenReady(block: A.() -> Unit) =
    receive().apply(block)
