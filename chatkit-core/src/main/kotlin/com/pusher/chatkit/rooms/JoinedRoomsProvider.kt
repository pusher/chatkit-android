package com.pusher.chatkit.rooms

import elements.Error // repackage, rename PusherError, alias ChatkitError
import java.util.*

data class Room private constructor( // JoinedRoom?
        val id: String,
        val name: String,
        val isPrivate: Boolean,
        val createdById: String,
        val unreadCount: Int?,
        val lastMessageAt: String?,
        val createdAt: Date,
        val updatedAt: Date,
        val deletedAt: Date?
)

sealed class JoinedRoomsState {
    data class Initializing(val error: Error?): JoinedRoomsState()

    data class Connected(
            val rooms: Set<Room>,
            val update: JoinedRoomsStateUpdate?
    ): JoinedRoomsState()

    data class Degraded(
            val rooms: Set<Room>,
            val update: JoinedRoomsStateUpdate?,
            // TODO: model degraded details
            val error: Error
    ): JoinedRoomsState()

    object Closed: JoinedRoomsState()
}

sealed class JoinedRoomsStateUpdate {
    data class JoinedRoom(val joinedRoom: Room): JoinedRoomsStateUpdate()
    data class RoomUpdated(val updatedRoom: Room, val previousValue: Room): JoinedRoomsStateUpdate()
    data class LeftRoom(val leftRoom: Room): JoinedRoomsStateUpdate()
    data class RoomDeleted(val deletedRoom: Room): JoinedRoomsStateUpdate()
}

class JoinedRoomsProvider /* TODO: check : Closeable */ { // Repository?

    val rooms: Set<Room> get() = setOf() // get from the store and map

    fun observe(observer: (JoinedRoomsState) -> Unit) { }

    fun close() {}

}


