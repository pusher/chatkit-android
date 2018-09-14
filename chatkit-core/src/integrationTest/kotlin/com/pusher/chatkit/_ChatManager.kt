package com.pusher.chatkit

import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.test.FutureValue

/**
 * Waits for connection to be stabilised before waiting for a given event
 */
fun <A> ChatManager.connectFor(block: (ChatManagerEvent) -> A?): FutureValue<A> {
    val futureValue = FutureValue<A>()
    var ready by FutureValue<Any>()
    connect {
        if (it is ChatManagerEvent.CurrentUserReceived) ready = it
        (block(it))?.let { futureValue.set(it) }
    }
    checkNotNull(ready)
    return futureValue
}

/**
 * Same as [connectFor] but for room subs
 */
fun <A> ChatManager.subscribeRoomFor(roomName: String, block: (RoomEvent) -> A?): FutureValue<A> {
    val currentUserEvent by connectFor { it as? ChatManagerEvent.CurrentUserReceived }
    return currentUserEvent.currentUser.subscribeRoomFor(roomName, block)
}

/**
 * Same as [connectFor] but for room subs
 */
fun <A> CurrentUser.subscribeRoomFor(roomName: String, block: (RoomEvent) -> A?): FutureValue<A> {
    val futureValue = FutureValue<A>()
    var ready by FutureValue<Any>()
    val room = rooms.first { it.name == roomName }
    subscribeToRoom(room) {
        if (it is RoomEvent.InitialReadCursors) ready = it
        (block(it))?.let { futureValue.set(it) }
    }
    checkNotNull(ready)
    return futureValue
}
