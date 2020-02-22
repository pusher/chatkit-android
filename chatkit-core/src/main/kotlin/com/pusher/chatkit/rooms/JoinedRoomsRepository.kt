@file:Suppress("unused") // TODO: remove when no longer just a sketch (unused)

package com.pusher.chatkit.rooms

import elements.Error

sealed class JoinedRoomsState {

    data class Initializing(val error: Error? = null) : JoinedRoomsState()

    data class Connected(
        val rooms: Set<Room>,
        val changeDescription: ChangeDescription?
    ) : JoinedRoomsState()

    data class Degraded(
        val rooms: Set<Room>,
        val changeDescription: ChangeDescription?,
        val error: Error
    ) : JoinedRoomsState()

    data class Closed(val error: Error? = null) : JoinedRoomsState()

    sealed class ChangeDescription {
        data class JoinedRoom(val joinedRoom: Room) : ChangeDescription()
        data class RoomUpdated(val updatedRoom: Room, val previousValue: Room) : ChangeDescription()
        data class LeftRoom(val leftRoom: Room) : ChangeDescription()
        data class RoomDeleted(val deletedRoom: Room) : ChangeDescription()
    }
}

class JoinedRoomsRepository /* TODO: check : Closeable */ {

    val rooms: Set<Room> get() = setOf() // get from the store and map

    fun observe(observer: (JoinedRoomsState) -> Unit) {
        // observe the relevant sub-store and translate delegating stuff to a mapper
        observer(JoinedRoomsState.Initializing())
    }

    fun close() {}
}
