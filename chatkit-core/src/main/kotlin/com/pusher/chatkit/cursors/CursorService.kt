package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.util.Throttler
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.flatMap
import com.pusher.platform.network.toFuture
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal class CursorService(
        private val client: PlatformClient,
        private val logger: Logger
) {
    private val cursorsStore = CursorsStore()
    private val subscriptions = mutableListOf<Subscription>()

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

    fun getReadCursor(userId: String, roomId: Int) : Future<Result<Cursor, Error>> =
        (cursorsStore[userId][roomId]?.asSuccess<Cursor, Error>() ?: notSubscribedToRoom("$roomId").asFailure())
            .toFuture()

    private fun notSubscribedToRoom(name: String) =
        Errors.other("Must be subscribed to room $name to access member's read cursorsStore")

    fun saveCursors(cursors: Map<Int, Cursor>) {
        for ((_, cursor) in cursors) {
            this.cursorsStore[cursor.userId] += cursor
        }
    }

    fun subscribeForRoom(roomId: Int, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
        CursorSubscription(
            client,
            "/cursors/0/rooms/$roomId",
            cursorsStore,
            consumeEvent,
            logger
        ).also { subscriptions.add(it) }.connect()

    fun subscribeForUser(userId: String, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
        CursorSubscription(
            client,
            "/cursors/0/users/$userId",
            cursorsStore,
            consumeEvent,
            logger
        ).also { subscriptions.add(it) }.connect()

    fun request(userId: String): Future<Result<List<Cursor>, Error>> = client.doGet(
        "/cursors/0/users/$userId",
        responseParser = { it.parseAs<List<Cursor>>() }
    )

    fun unsubscribe() = subscriptions.forEach { sub -> sub.unsubscribe() }
}