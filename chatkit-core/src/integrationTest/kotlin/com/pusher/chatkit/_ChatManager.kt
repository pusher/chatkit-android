package com.pusher.chatkit

import com.pusher.chatkit.rooms.RoomSubscriptionEvent
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
fun <A> ChatManager.subscribeRoomFor(roomName: String, block: (RoomSubscriptionEvent) -> A?): FutureValue<A> {
    val futureValue = FutureValue<A>()
    var ready by FutureValue<Any>()
    val currentUserEvent by connectFor { it as? ChatManagerEvent.CurrentUserReceived }

    val currentUser = currentUserEvent.currentUser
    val room = currentUser.rooms.first { it.name == roomName }

    currentUser.subscribeToRoom(room) {
        if (it is RoomSubscriptionEvent.InitialReadCursors) ready = it
        (block(it))?.let { futureValue.set(it) }
    }
    checkNotNull(ready)
    return futureValue
}
