@file:Suppress("unused", "MemberVisibilityCanBePrivate") // public entry point

package com.pusher.chatkit

import com.pusher.chatkit.messages.MessagesRepository
import com.pusher.chatkit.rooms.JoinedRoomsRepository
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error

class ChatkitCoreConnector(
    private val instanceLocator: String,
    private val tokenProvider: TokenProvider
) {

    fun connect(resultHandler: (Result<ChatkitCore, Error>) -> Unit) {
        // ...
        resultHandler(ChatkitCore(User()).asSuccess())
    }
}

interface ChatkitRepositoryFactory {

    val status: Status

    fun createJoinedRoomRepository(): JoinedRoomsRepository
    fun createJoinedRoomRepositoryFactory(): JoinedRoomsRepositoryFactory

    fun createMessagesRepository(roomId: Int): MessagesRepository
    fun createMessagesRepositoryFactory(roomId: Int): MessagesRepositoryFactory

    fun close()
}

class ChatkitCore internal constructor(val currentUser: User) : ChatkitRepositoryFactory {

    private var _status: Status = Status.Connecting()
    override val status get() = _status

    override fun createJoinedRoomRepository() = JoinedRoomsRepository()
    override fun createJoinedRoomRepositoryFactory() = JoinedRoomsRepositoryFactory()

    override fun createMessagesRepository(roomId: Int) = MessagesRepository()
    override fun createMessagesRepositoryFactory(roomId: Int) = MessagesRepositoryFactory()

    override fun close() {
        _status = Status.Closed()
    }
}

sealed class Status {
    data class Connecting(val error: Error? = null) : Status()
    object Connected : Status()
    data class Closed(val error: Error? = null) : Status()
}

class JoinedRoomsRepositoryFactory // TODO: impl, move
class MessagesRepositoryFactory // TODO: impl, move
