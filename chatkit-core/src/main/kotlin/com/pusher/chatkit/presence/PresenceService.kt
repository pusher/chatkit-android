package com.pusher.chatkit.presence

import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.users.UserService
import com.pusher.platform.logger.Logger

internal class PresenceService(
        client: PlatformClient,
        userId: String,
        consumeEvent: (ChatManagerEvent) -> Unit,
        userService: UserService,
        logger: Logger
) {
    private val subscription = PresenceSubscription(
            client,
            userId,
            consumeEvent,
            userService,
            logger
    )

    fun subscribe() = subscription.connect()

    fun unsubscribe() = subscription.unsubscribe()
}
