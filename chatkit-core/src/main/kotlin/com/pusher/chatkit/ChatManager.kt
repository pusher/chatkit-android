package com.pusher.chatkit

import com.pusher.platform.network.Futures
import com.pusher.util.Result
import com.pusher.util.asFailure
import elements.Error
import elements.Errors

typealias CustomData = Map<String, Any>

class ChatManager(
        instanceLocator: String,
        userId: String,
        dependencies: ChatkitDependencies
) {
    private val syncChatManager = SynchronousChatManager(
            instanceLocator,
            userId,
            dependencies
    )

    fun connect(listeners: ChatListeners, callback: (Result<CurrentUser, Error>) -> Unit) =
            connect(listeners.toCallback(), callback)

    @JvmOverloads
    fun connect(consumer: ChatManagerEventConsumer = {}, callback: (Result<CurrentUser, Error>) -> Unit) {
        makeCallback(
                f = { syncChatManager.connect(consumer).map { CurrentUser(it) } },
                c = callback
        )
    }

    /**
     * Tries to close all pending subscriptions and resources
     */
    fun close(callback: (Result<Unit, Error>) -> Unit) {
        makeCallback(
                f = { syncChatManager.close() },
                c = callback
        )
    }

    fun disablePushNotifications(callback: (Result<Unit, Error>) -> Unit) {
        makeCallback(
                f = { syncChatManager.disablePushNotifications() },
                c = callback
        )
    }

    /**
     * If you would prefer calls to block and to manage your own concurrency with threading or
     * coroutines, this returns a chatmanager with synchronous interface.
     *
     * e.g.
     *
     *   chatManager.connect(listeners) { result ->
     *     // result has your currentUser object
     *   }
     *   // this line executes before connect is complete
     *
     * vs
     *
     *   val result = chatManager.blocking().connect(listeners)
     *   // result has your currentUser object
     *   // this line does not execute connect has completed
     */
    fun blocking() = syncChatManager
}

fun <V> makeCallback(f: () -> Result<V, Error>, c: (Result<V, Error>) -> Unit) {
    Futures.schedule {
        c.invoke(try {
            f()
        } catch (e: Throwable) {
            Errors.other(e).asFailure<V, Error>()
        })
    }
}

fun <V> makeSingleCallback(f: () -> V, c: (V) -> Unit) {
    Futures.schedule {
            c.invoke(f())
    }
}
