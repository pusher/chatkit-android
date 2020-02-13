@file:Suppress("unused", "MemberVisibilityCanBePrivate") // public entry point

package com.pusher.chatkit

import com.pusher.chatkit.messages.MessagesProvider
import com.pusher.chatkit.rooms.JoinedRoomsProvider
import com.pusher.chatkit.rooms.JoinedRoomsViewModel
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error

class ChatkitConnector(
    private val instanceLocator: String,
    private val tokenProvider: TokenProvider
) {

    fun connect(resultHandler: (Result<Chatkit, Error>) -> Unit) {
        // ...
        resultHandler(Chatkit(User()).asSuccess())
    }
}

class Chatkit internal constructor(val currentUser: User) {

    private var _status: Status = Status.Connecting()
    val status get() = _status

    fun createJoinedRoomProvider() = JoinedRoomsProvider()

    fun createJoinedRoomViewModel() = JoinedRoomsViewModel(createJoinedRoomProvider())

    fun createMessagesProvider() = MessagesProvider()

    fun close() {
        _status = Status.Closed()
    }
}

sealed class Status {
    data class Connecting(val error: Error? = null) : Status()
    object Connected : Status()
    data class Closed(val error: Error? = null) : Status()
}
