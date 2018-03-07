package com.pusher.chatkit.channels

import com.pusher.chatkit.UsesCoroutines
import elements.Subscription
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel

@UsesCoroutines
fun <A> broadcastToChannel(onSubscribe: BroadcastChannel<A>.() -> Subscription): ReceiveChannel<A> =
    BroadCastChannelWithSubscription(onSubscribe)

@UsesCoroutines
private class BroadCastChannelWithSubscription<A>(
    onSubscribe: BroadcastChannel<A>.() -> Subscription,
    val broadcastChannel: BroadcastChannel<A> = BroadcastChannel(Channel.CONFLATED),
    val receiverChannel: SubscriptionReceiveChannel<A> = broadcastChannel.openSubscription()
): ReceiveChannel<A> by receiverChannel {

    val subscription = broadcastChannel.onSubscribe()

    override fun cancel(cause: Throwable?): Boolean {
        subscription.unsubscribe()
        return receiverChannel.cancel(cause)
    }

}
