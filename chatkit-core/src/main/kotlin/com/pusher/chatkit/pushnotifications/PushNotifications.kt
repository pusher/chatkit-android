package com.pusher.chatkit.pushnotifications

import com.pusher.chatkit.PlatformClient
import com.pusher.util.Result
import elements.Error

interface PushNotifications {
  fun start(
          instanceId: String,
          beamsTokenProviderService: BeamsTokenProviderService
  ): Result<Unit, Error>
  fun setUserId(userId: String): Result<Unit, Error>
  fun stop(): Result<Unit, Error>
}
