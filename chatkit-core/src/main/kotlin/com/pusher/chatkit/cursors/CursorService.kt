package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.InstanceType
import com.pusher.chatkit.InstanceType.*
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.flatMap
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import com.pusher.platform.network.toFuture
import elements.Error
import elements.Errors
import elements.SubscriptionEvent
import java.util.concurrent.Future

internal class CursorService(private val chatManager: ChatManager) {

    private val cursors = CursorsStore()

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
        cursors[userId] += Cursor(
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
        (cursors[userId][roomId]?.asSuccess<Cursor, Error>() ?: notSubscribedToRoom("$roomId").asFailure())
            .toFuture()

    private fun notSubscribedToRoom(name: String) =
        Errors.other("Must be subscribed to room $name to access member's read cursors")

    fun saveCursors(cursors: Map<Int, Cursor>) {
        cursors.forEach { _, cursor -> this.cursors[cursor.userId] += cursor }
    }

    fun subscribeForRoom(roomId: Int, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
        createSubscription("/cursors/0/rooms/$roomId", consumeEvent)

    fun subscribeForUser(userId: String, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
        createSubscription("/cursors/0/users/$userId", consumeEvent)

    private fun createSubscription(
        path: String,
        consumeEvent: (CursorSubscriptionEvent) -> Unit
    ) = chatManager.subscribeResuming(
        path = path,
        listeners = SubscriptionListeners<ChatEvent>(
            onEvent = { event ->
                val cursorEvent = event.toCursorEvent()
                    .recover { CursorSubscriptionEvent.OnError(it) }
                consumeEvent(cursorEvent)
                when(cursorEvent) {
                    is CursorSubscriptionEvent.OnCursorSet ->  cursors[cursorEvent.cursor.userId] += cursorEvent.cursor
                    is CursorSubscriptionEvent.InitialState -> cursors += cursorEvent.cursors
                }
            },
            onError = { consumeEvent(CursorSubscriptionEvent.OnError(it)) }
        ),
        messageParser = { it.parseAs() },
        instanceType = CURSORS
    )

    fun request(userId: String): Future<Result<List<Cursor>, Error>> = chatManager.doGet(
        "/cursors/0/users/$userId",
        responseParser = { it.parseAs<List<Cursor>>() },
        instanceType = CURSORS
    )

}

private fun SubscriptionEvent<ChatEvent>.toCursorEvent(): Result<CursorSubscriptionEvent, Error> = when (body.eventName) {
    "new_cursor" -> body.data.parseAs<Cursor>().map(CursorSubscriptionEvent::OnCursorSet)
    "initial_state" -> body.data.parseAs<CursorSubscriptionEvent.InitialState>()
    else -> CursorSubscriptionEvent.NoEvent.asSuccess<CursorSubscriptionEvent, Error>()
}.map { it } // generics -.-

private class CursorsStore {

    private val map = mutableMapOf<String, UserCursorStore>()

    operator fun get(userId: String) =
        map[userId] ?: UserCursorStore().also { map[userId] = it }

    operator fun set(userId: String, cursor: Cursor) {
        get(userId) += cursor
    }

    operator fun plusAssign(cursors: List<Cursor>) =
        cursors.forEach { cursor -> this[cursor.userId] += cursor }

}

private class UserCursorStore {

    private val cursors = mutableMapOf<Int, Cursor>()

    operator fun plusAssign(cursor: Cursor) {
        cursors[cursor.roomId] = cursor
    }

    operator fun get(roomId: Int) = cursors[roomId]

}
