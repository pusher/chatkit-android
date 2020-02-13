package com.pusher.chatkit.pushnotifications

interface PushNotificationsFactory {
    fun newBeams(
        instanceId: String,
        beamsTokenProviderService: BeamsTokenProviderService
    ): PushNotifications
}
