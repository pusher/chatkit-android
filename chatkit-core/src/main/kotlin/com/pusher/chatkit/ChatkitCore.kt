package com.pusher.chatkit

import com.pusher.chatkit.api.createPlatformClientModule
import com.pusher.chatkit.users.api.createUserSubscriberModule
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ChatkitCoreConnector {

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
                module { single { dependencies.logger }},
                createUserSubscriberModule()
            )
            modules(overrideModules)
        }
    }

    private val instanceLocator: String
    private val dependencies: ChatkitDependencies
    private val chatkitKoinApplication: KoinApplication

    fun connect(resultHandler: (Result<ChatkitCore, Error>) -> Unit) {
        // ...
        resultHandler(ChatkitCore().asSuccess())
    }

}

class ChatkitCore
