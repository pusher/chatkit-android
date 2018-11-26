package com.pusher.chatkit.test

import com.pusher.chatkit.pushnotifications.BeamsTokenProviderService
import com.pusher.chatkit.pushnotifications.PushNotifications
import com.pusher.chatkit.pushnotifications.PushNotificationsFactory
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error

class NoPushNotificationFactory: PushNotificationsFactory {
    override fun newBeams(instanceId: String, beamsTokenProviderService: BeamsTokenProviderService): PushNotifications {
        return object : PushNotifications {
            override fun start(): Result<Unit, Error> {
                return Unit.asSuccess()
            }

            override fun setUserId(userId: String): Result<Unit, Error> {
                return Unit.asSuccess()
            }

            override fun stop(): Result<Unit, Error> {
                return Unit.asSuccess()
            }
        }
    }
}