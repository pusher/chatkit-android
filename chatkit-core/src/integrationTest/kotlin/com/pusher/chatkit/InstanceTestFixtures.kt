package com.pusher.chatkit

private fun systemProperty(name: String) =
    System.getProperty(name) ?: "Missing gradle/system property '$name'"

/**
 * These should be added in ~/.gradle/gradle.properties or as a system property. If running them on the IDE, they must be a system property.
 */
val INSTANCE_LOCATOR: String = systemProperty("chatkit_integration_locator")
val INSTANCE_ID = INSTANCE_LOCATOR.split(":").getOrNull(2) ?: "Missing instance id in locator (property 'chatkit_integration_locator')"

val AUTH_KEY: String = systemProperty("chatkit_integration_key")
val AUTH_KEY_ID: String = AUTH_KEY.split(":")[0]
val AUTH_KEY_SECRET: String = AUTH_KEY.split(":")[1]

object Users {
    const val SUPER_USER = "super-user" // sudo access
    const val PUSHERINO = "pusherino"
    const val ALICE = "alice"
}
