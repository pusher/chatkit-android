package com.pusher.chatkit

import com.pusher.chatkit.api.createPlatformClientModule
import com.pusher.chatkit.state.createStoreModule
import com.pusher.chatkit.users.api.createUserSubscriberModule
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The entry point for JVM (non-Android) apps to connect with the backend service
 * and retrieve [ChatkitCore] instance for further interaction with the SDK.
 */
class ChatkitCoreConnector {

    /**
     * Creates an object of this class for your Chatkit instance
     * using passed [TokenProvider][com.pusher.platform.tokenProvider.TokenProvider]
     * (for authentication and user identification) and additional optional dependencies.
     *
     * @param instanceLocator value that can be found in
     * the [dashboard](https://dash.pusher.com/chatkit/)
     * (under the Credentials tab of a selected instance)
     * @param dependencies used for passing your [TokenProvider][com.pusher.platform.tokenProvider.TokenProvider]
     * alongside optional dependencies like your [Logger][com.pusher.platform.logger.Logger]
     */
    constructor(
        instanceLocator: String,
        dependencies: ChatkitDependencies
    ) : this(instanceLocator, dependencies, overrideModules = emptyList())

    internal constructor(
        instanceLocator: String,
        dependencies: ChatkitDependencies,
        overrideModules: List<Module>
    ) {
        this.instanceLocator = instanceLocator
        this.dependencies = dependencies

        chatkitKoinApplication = koinApplication {
            modules(
                createPlatformClientModule(instanceLocator, dependencies),
                module { single { dependencies.logger } },
                createUserSubscriberModule(),

                createStoreModule()
            )
            modules(overrideModules)
        }
    }

    private val instanceLocator: String
    private val dependencies: ChatkitDependencies
    private val chatkitKoinApplication: KoinApplication

    /**
     * Asynchronously connects with the backend and provide [ChatkitCore] as the entry point
     * to interact with the SDK.
     *
     * @param resultHandler callback for handling successful connection, or errors while
     * trying to connect
     */
    fun connect(resultHandler: (Result<ChatkitCore, Error>) -> Unit) {
        // TODO: implement
        resultHandler(ChatkitCore().asSuccess())
    }
}

/**
 * The entry point to interact with the SDK after connecting.
 *
 * Use [ChatkitCoreConnector] to retrieve an instance of this class.
 */
class ChatkitCore
