package com.pusher.chatkit

import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error

class ChatkitCoreConnector(
    private val instanceLocator: String,
    private val dependencies: ChatkitDependencies
) {

    fun connect(resultHandler: (Result<ChatkitCore, Error>) -> Unit) {
        // ...
        resultHandler(ChatkitCore().asSuccess())
    }

}

class ChatkitCore {

}
