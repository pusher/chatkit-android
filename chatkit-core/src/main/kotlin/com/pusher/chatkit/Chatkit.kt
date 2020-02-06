@file:Suppress("unused", "MemberVisibilityCanBePrivate")  // public entry point

package com.pusher.chatkit

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

    fun connect(): Result<Chatkit, Error> {
        return Chatkit(User()).asSuccess()
    }

}

class Chatkit internal constructor(val currentUser: User) {

    private var _connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected
    val connectionStatus get() = _connectionStatus

    fun createJoinedRoomProvider(): JoinedRoomsProvider =
        JoinedRoomsProvider()

    fun createJoinedRoomViewModel(): JoinedRoomsViewModel =
            JoinedRoomsViewModel(createJoinedRoomProvider())

    fun disconnect() {
        _connectionStatus = ConnectionStatus.Disconnected
    }

}