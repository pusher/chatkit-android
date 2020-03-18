@file:Suppress("unused", "MemberVisibilityCanBePrivate") // public entry point

package com.pusher.chatkit

import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error

/**
 * The entry point for Android apps to connect with the backend service
 * and retrieve [Chatkit] instance for further interaction with the SDK.
 */
class ChatkitConnector(
    instanceLocator: String,
    dependencies: AndroidChatkitDependencies
) {

    private val chatkitCoreConnector = ChatkitCoreConnector(instanceLocator, dependencies)

    /**
     * Asynchronously connects with the backend and provide [Chatkit] as the entry point
     * to interact with the SDK.
     *
     * @param resultHandler callback for handling successful connection, or errors while
     * trying to connect
     */
    fun connect(resultHandler: (Result<Chatkit, Error>) -> Unit) {
        chatkitCoreConnector.connect { result ->
            result.fold(
                onSuccess = { chatkitCore ->
                    resultHandler(Chatkit(chatkitCore).asSuccess())
                },
                onFailure = { error ->
                    resultHandler(error.asFailure())
                }
            )
        }
    }
}

/**
 * The entry point to interact with the SDK after connecting.
 *
 * Use [ChatkitConnector] to retrieve an instance of this class.
 */
class Chatkit internal constructor(private val chatkitCore: ChatkitCore)
