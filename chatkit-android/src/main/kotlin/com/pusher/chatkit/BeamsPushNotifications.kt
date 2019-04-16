package com.pusher.chatkit

import android.content.Context
import com.pusher.chatkit.pushnotifications.BeamsTokenProviderService
import com.pusher.chatkit.pushnotifications.PushNotifications
import com.pusher.chatkit.util.FutureValue
import com.pusher.pushnotifications.BeamsCallback
import com.pusher.pushnotifications.PusherCallbackError
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.util.Result
import elements.Error
import com.pusher.pushnotifications.PushNotifications as Beams

// Implementation of `PushNotifications` interface using Pusher Beams!
class BeamsPushNotifications(
        private val context: Context,
        private val instanceId: String,
        private val beamsTokenProviderService: BeamsTokenProviderService
) : PushNotifications {

    private val tokenProvider = object : TokenProvider {
        override fun fetchToken(userId: String): String =
                beamsTokenProviderService.fetchToken(userId)
    }

    override fun start(): Result<Unit, Error> {
        return try {
            Beams.start(context, instanceId)
            Result.success(Unit)
        } catch (ex: ClassNotFoundException) {
            Result.failure(elements.OtherError("It seems like some dependencies that Push Notifications requires are missing. Check https://docs.pusher.com/chatkit for more information.", ex.cause))
        } catch (ex: Throwable) {
            Result.failure(elements.OtherError(ex.message ?: "Unknown error", ex.cause))
        }
    }

    override fun setUserId(userId: String): Result<Unit, Error> {
        val f = FutureValue<Result<Unit, Error>>()
        try {
            Beams.setUserId(userId, tokenProvider, object : BeamsCallback<Void, PusherCallbackError> {
                override fun onFailure(error: PusherCallbackError) {
                    f.set(Result.failure(elements.OtherError(error.message, error.cause)))
                }

                override fun onSuccess(vararg values: Void) {
                    f.set(Result.success(Unit))
                }
            })
        } catch (ex: ClassNotFoundException) {
            return Result.failure(elements.OtherError("It seems like some dependencies that Push Notifications requires are missing. Check https://docs.pusher.com/chatkit for more information.", ex.cause))
        } catch (ex: Throwable) {
            return Result.failure(elements.OtherError(ex.message ?: "Unknown error", ex.cause))
        }

        return f.get()
    }

    override fun stop(): Result<Unit, Error> {
        try {
            Beams.stop()
        } catch (ex: ClassNotFoundException) {
            return Result.failure(elements.OtherError("It seems like some dependencies that Push Notifications requires are missing. Check https://docs.pusher.com/chatkit for more information.", ex.cause))
        } catch (ex: Throwable) {
            return Result.failure(elements.OtherError(ex.message ?: "Unknown error", ex.cause))
        }
        return Result.success(Unit)
    }
}
