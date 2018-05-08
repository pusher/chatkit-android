package com.pusher.chatkit.cursors

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

internal fun ChatManager.cursorService(currentUser: CurrentUser) =
    CursorService(this, currentUser)

internal class CursorService(
    chatManager: ChatManager,
    private val currentUser: CurrentUser // TODO: remove dependency to user
) {

    private val cursorsInstance = chatManager.cursorsInstance
    private val cursors = currentUser.cursors
    private val tokenProvider = chatManager.tokenProvider
    private val tokenParams = chatManager.dependencies.tokenParams

    fun setReadCursor(
        roomId: Int,
        position: Int
    ): Future<Result<Boolean, Error>> = cursorsInstance.request<String>(
        options = RequestOptions(
            method = "PUT",
            path = "/cursors/0/rooms/$roomId/users/${currentUser.id}",
            body = ChatManager.GSON.toJson(SetCursorRequest(position))
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        responseParser = { it.parseAs() }
    ).mapResult { true }

    fun getReadCursor(roomId: Int) : Result<Cursor, Error> =
        roomId.cursor?.asSuccess() ?: notSubscrivedToRoom("$roomId").asFailure()

    fun getReadCursor(room: Room) : Result<Cursor, Error> =
        room.id.cursor?.asSuccess() ?: notSubscrivedToRoom(room.name).asFailure()

    private val Int.cursor: Cursor?
        get() = cursors.takeIf { currentUser.isSubscribedToRoom(this) }?.get(this)

    private fun notSubscrivedToRoom(name: String) =
        Errors.other("Must be subscribed to room $name to access member's read cursors")
}
