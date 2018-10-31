package com.pusher.chatkit

private fun systemProperty(name: String): String =
    checkNotNull(System.getProperty(name)) { "Missing gradle/system property '$name'" }

/**
 * These should be added in ~/.gradle/gradle.properties or as a system property. If running them on the IDE, they must be a system property.
 */
val INSTANCE_LOCATOR by lazy { systemProperty("chatkit_integration_locator") }
val INSTANCE_ID by lazy { INSTANCE_LOCATOR.split(":").getOrNull(2) ?: "Missing instance id in locator (property 'chatkit_integration_locator')" }

val AUTH_KEY by lazy { systemProperty("chatkit_integration_key") }
val AUTH_KEY_ID by lazy { AUTH_KEY.split(":")[0] }
val AUTH_KEY_SECRET by lazy { AUTH_KEY.split(":")[1] }

object Users {
    const val SUPER_USER = "super-user" // sudo access
    const val PUSHERINO = "pusherino"
    const val ALICE = "alice"
}


object Rooms {
    const val GENERAL = "general"
    const val NOT_GENERAL = "not-general"
    val SAMPLE_CUSTOM_DATA = mapOf("custom" to "data")
}
