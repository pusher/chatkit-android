package com.pusher.chatkit.api

import com.pusher.platform.Instance
import com.pusher.platform.PlatformDependencies
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

typealias InstanceLocator = String

internal val createPlatformClientModule: (InstanceLocator, PlatformDependencies) -> Module =
    { locator, dependencies ->
        module {
            singlePlatformClient(locator, InstanceType.CORE, dependencies)
        }
    }

private inline fun <reified T : InstanceType> Module.singlePlatformClient(
    locator: InstanceLocator,
    instanceType: T,
    dependencies: PlatformDependencies
) {
    single(named(instanceType)) {
        createPlatformInstance(
            locator,
            instanceType,
            dependencies
        )
    }
}

private fun createPlatformInstance(
    locator: String,
    type: InstanceType,
    dependencies: PlatformDependencies
) = Instance(
    locator,
    type.serviceName,
    type.version,
    dependencies
)
