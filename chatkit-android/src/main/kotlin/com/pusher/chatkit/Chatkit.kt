@file:Suppress("unused", "MemberVisibilityCanBePrivate") // public entry point

package com.pusher.chatkit

import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error

class ChatkitConnector(
    instanceLocator: String,
    dependencies: AndroidChatkitDependencies
) {

    private val chatkitCoreConnector = ChatkitCoreConnector(instanceLocator, dependencies)

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

class Chatkit internal constructor(private val chatkitCore: ChatkitCore)