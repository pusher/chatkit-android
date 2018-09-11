package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.util.Throttler
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.flatMap
import com.pusher.platform.network.toFuture
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Errors
import java.util.concurrent.Future

class CursorService(
        private val client: PlatformClient,
        private val logger: Logger
) {
    private val cursorsStore = CursorsStore()

    fun setReadCursor(
        userId: String,
        roomId: Int,
        position: Int
    ): Future<Result<Unit, Error>> =
            setReadCursorThrottler.throttle(
                RequestOptions(
                    method = "PUT",
                    path = "/cursors/0/rooms/$roomId/users/$userId",
                    body = """{ "position" : $position }"""
                )
            ).flatMap { it }
            .mapResult {
                cursorsStore[userId] += Cursor(
                        userId = userId,
                        roomId = roomId,
                        position = position
                )
            }

    private val setReadCursorThrottler = Throttler { options: RequestOptions ->
        client.doRequest<JsonElement>(
            options = options,
            responseParser = { it.parseAs() }
        )
    }

    // TODO not an async operation?
    fun getReadCursor(userId: String, roomId: Int) : Future<Result<Cursor, Error>> =
        (cursorsStore[userId][roomId]?.asSuccess<Cursor, Error>() ?: notSubscribedToRoom("$roomId").asFailure())
            .toFuture()

    private fun notSubscribedToRoom(name: String) =
        Errors.other("Must be subscribed to room $name to access member's read cursorsStore")

    fun subscribeForRoom(
            roomId: Int,
            externalConsumer: (CursorSubscriptionEvent) -> Unit
    ) = cursorSubscription(
            "/cursors/0/rooms/$roomId",
            listOf(::applySideEffects, externalConsumer)
    )

    fun subscribeForUser(
            userId: String,
            externalConsumer: (CursorSubscriptionEvent) -> Unit
    ) = cursorSubscription(
            "/cursors/0/users/$userId",
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