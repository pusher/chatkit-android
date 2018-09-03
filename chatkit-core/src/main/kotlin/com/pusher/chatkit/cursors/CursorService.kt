package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.InstanceType.*
import com.pusher.chatkit.util.Throttler
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.flatMap
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import com.pusher.platform.network.toFuture
import elements.Error
import elements.Errors
import java.util.concurrent.Future

internal class CursorService(private val chatManager: ChatManager) {
    private val cursorsStore = CursorsStore()

    fun setReadCursor(
        userId: String,
        roomId: Int,
        position: Int
    ): Future<Result<Unit, Error>> = setReadCursorThrottler.throttle(RequestOptions(
        method = "PUT",
        path = "/cursors/0/rooms/$roomId/users/$userId",
        body = """{ "position" : $position }"""
    ))
    .flatMap { it }
    .mapResult {
        cursorsStore[userId] += Cursor(
            userId = userId,
            roomId = roomId,
            position = position
        )
    }

    private val setReadCursorThrottler = Throttler { options: RequestOptions ->
        chatManager.platformInstance(CURSORS).request<JsonElement>(
            options = options,
            tokenProvider = chatManager.tokenProvider,
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
            "/cursors/0/rooms/$roomId",
            chatManager,
            cursorsStore,
            consumeEvent
        ).connect()

    fun subscribeForUser(userId: String, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
        CursorSubscription(
            "/cursors/0/users/$userId",
            chatManager,
            cursorsStore,
            consumeEvent
        ).connect()


    fun request(userId: String): Future<Result<List<Cursor>, Error>> = chatManager.doGet(
        "/cursors/0/users/$userId",
        responseParser = { it.parseAs<List<Cursor>>() },
        instanceType = CURSORS
    )

}

