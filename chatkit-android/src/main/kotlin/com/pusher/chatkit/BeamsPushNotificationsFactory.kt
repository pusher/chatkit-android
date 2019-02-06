package com.pusher.chatkit

import android.content.Context
import com.pusher.chatkit.pushnotifications.BeamsTokenProviderService
import com.pusher.chatkit.pushnotifications.PushNotifications
import com.pusher.chatkit.pushnotifications.PushNotificationsFactory

class BeamsPushNotificationsFactory(
        private val context: Context
) : PushNotificationsFactory {

    override fun newBeams(
            instanceId: String,
            beamsTokenProviderService: BeamsTokenProviderService
    ): PushNotifications =
            BeamsPushNotifications(context, instanceId, beamsTokenProviderService)
}