package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.util.Throttler
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Errors
import java.net.URLEncoder

class CursorService(
        private val client: PlatformClient,
        private val logger: Logger
) {
    private val cursorsStore = CursorsStore()

    private val setReadCursorThrottler =
            Throttler { options: RequestOptions ->
                client.doRequest<JsonElement>(
                        options = options,
                        responseParser = { it.parseAs() }
                )
            }

    fun setReadCursor(
        userId: String,
        roomId: String,
        position: Int
    ) =
        setReadCursorThrottler.throttle(
                RequestOptions(
                        method = "PUT",
                        path = "/cursors/0/rooms/$roomId/users/$userId",
                        body = """{ "position" : $position }"""
                )
        ).mapResult {
            cursorsStore[userId] += Cursor(
                    userId = userId,
                    roomId = roomId,
                    position = position
            )
        }

    fun getReadCursor(userId: String, roomId: String) : Result<Cursor, Error> =
        (cursorsStore[userId][roomId]?.asSuccess() ?: notSubscribedToRoom(roomId).asFailure())

    private fun notSubscribedToRoom(name: String) =
        Errors.other("Must be subscribed to room $name to access member's read cursors")

    fun subscribeForRoom(
            roomId: String,
            externalConsumer: (CursorSubscriptionEvent) -> Unit
    ) = cursorSubscription(
            "/cursors/0/rooms/${URLEncoder.encode(roomId, "UTF-8")}",
            listOf(::applySideEffects, externalConsumer)
    )

    fun subscribeForUser(
            userId: String,
            externalConsumer: (CursorSubscriptionEvent) -> Unit
    ) = cursorSubscription(
            "/cursors/0/users/${URLEncoder.encode(userId, "UTF-8")}",
            listOf(::applySideEffects, externalConsumer)
    )

    private fun cursorSubscription(
            path: String,
            consumers: List<CursorSubscriptionConsumer>
    ) = ResolvableSubscription(
            client = client,
            path = path,
            listeners = SubscriptionListeners(
                    onEvent = { event -> consumers.forEach { consumer -> consumer(event.body) } },
                    onError = { error -> consumers.forEach { consumer -> consumer(CursorSubscriptionEvent.OnError(error)) } }
            ),
            messageParser = CursorSubscriptionEventParser,
            description = "Cursor $path",
            logger = logger
    )

    private fun applySideEffects(event: CursorSubscriptionEvent) {
        when (event) {
            is CursorSubscriptionEvent.OnCursorSet ->
                cursorsStore[event.cursor.userId] += event.cursor
            is CursorSubscriptionEvent.InitialState ->
                cursorsStore += event.cursors
        }
    }
}