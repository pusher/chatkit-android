package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Errors
import java.util.concurrent.Future

internal class CursorService(
    private val chatManager: ChatManager
) {

    private val cursorsInstance = chatManager.cursorsInstance
    private val tokenProvider = chatManager.tokenProvider
    private val tokenParams = chatManager.dependencies.tokenParams

    private val cursors = CursorsStore()

    fun setReadCursor(
        userId: String,
        roomId: Int,
        position: Int
    ): Future<Result<Boolean, Error>> = cursorsInstance.request<JsonElement>(
        options = RequestOptions(
            method = "PUT",
            path = "/cursors/0/rooms/$roomId/users/$userId",
            body = ChatManager.GSON.toJson(SetCursorRequest(position))
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        responseParser = { it.parseAs() }
    ).mapResult {
        cursors[userId] += Cursor(
            userId = userId,
            roomId = roomId,
            position = position
        )
        true
    }

    fun getReadCursor(userId: String, roomId: Int) : Result<Cursor, Error> =
        cursors[userId][roomId]?.asSuccess() ?: notSubscribedToRoom("$roomId").asFailure()

    internal fun subscribeToRoomCursors(roomId: Int, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
        CursorsSubscription.forRoom(chatManager, roomId) { event ->
            if (event is CursorSubscriptionEvent.OnCursorSet) {
                cursors[event.cursor.userId] += event.cursor
            }
            consumeEvent(event)
        }

    internal fun subscribeToCursors(userId: String, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
        CursorsSubscription.forUser(chatManager, userId, consumeEvent)

    private fun notSubscribedToRoom(name: String) =
        Errors.other("Must be subscribed to room $name to access member's read cursors")
}

internal class CursorsStore {

    private val map = mutableMapOf<String, UserCursorStore>()

    operator fun get(userId: String) =
        map[userId] ?: UserCursorStore().also { map[userId] = it }

    operator fun set(userId: String, cursor: Cursor) {
        get(userId) += cursor
    }

}

internal class UserCursorStore {

    private val cursors = mutableMapOf<Int, Cursor>()

    operator fun plusAssign(cursor: Cursor) {
        cursors[cursor.roomId] = cursor
    }

    operator fun get(roomId: Int) = cursors[roomId]

}
