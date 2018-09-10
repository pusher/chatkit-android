package com.pusher.chatkit.rooms

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.users.User
import elements.Error


typealias RoomConsumer = (RoomEvent) -> Unit

/**
 * Events which occur "at room scope". This is the public facing type for the consumption of "things
 * that happen in/to a room.
 *
 * Contrast with RoomSubscriptionEvent, the on-the-wire type for events received on a backend
 * RoomSubscription.
 */
sealed class RoomEvent {
    data class NewMessage(val message: Message) : RoomEvent()
    data class UserStartedTyping(val user: User) : RoomEvent()
    data class UserStoppedTyping(val user: User) : RoomEvent()
    data class UserJoined(val user: User) : RoomEvent()
    data class UserLeft(val user: User) : RoomEvent()
    data class UserCameOnline(val user: User) : RoomEvent()
    data class UserWentOffline(val user: User) : RoomEvent()
    data class InitialReadCursors(val cursor: List<Cursor>) : RoomEvent()
    data class NewReadCursor(val cursor: Cursor) : RoomEvent()
    data class RoomUpdated(val room: Room) : RoomEvent()
    data class RoomDeleted(val roomId: Int) : RoomEvent()
    data class ErrorOccurred(val error: Error) : RoomEvent()
    object NoEvent : RoomEvent()
}

/**
 * Callback listeners for events which occur "at room scope", these are an alternative way of
 * consuming RoomEvents.
 */
data class RoomListeners @JvmOverloads constructor(
        val onNewMessage: (Message) -> Unit = {},
        val onUserStartedTyping: (User) -> Unit = {},
        val onUserStoppedTyping: (User) -> Unit = {},
        val onUserJoined: (User) -> Unit = {},
        val onUserLeft: (User) -> Unit = {},
        val onUserCameOnline: (User) -> Unit = {},
        val onUserWentOffline: (User) -> Unit = {},
        val onNewReadCursor: (Cursor) -> Unit = {},
        val onRoomUpdated: (Room) -> Unit = {},
        val onRoomDeleted: (Int) -> Unit = {},
        val onErrorOccurred: (Error) -> Unit = {}
)

internal fun RoomListeners.toCallback(): RoomConsumer = { event ->
    when(event) {
        is RoomEvent.NewMessage -> onNewMessage(event.message)
        is RoomEvent.UserStartedTyping -> onUserStartedTyping(event.user)
        is RoomEvent.UserStoppedTyping -> onUserStoppedTyping(event.user)
        is RoomEvent.UserJoined -> onUserJoined(event.user)
        is RoomEvent.UserLeft -> onUserLeft(event.user)
        is RoomEvent.UserCameOnline -> onUserCameOnline(event.user)
        is RoomEvent.UserWentOffline -> onUserWentOffline(event.user)
        is RoomEvent.NewReadCursor -> onNewReadCursor(event.cursor)
        is RoomEvent.RoomUpdated -> onRoomUpdated(event.room)
        is RoomEvent.RoomDeleted -> onRoomDeleted(event.roomId)
        is RoomEvent.ErrorOccurred -> onErrorOccurred(event.error)
    }
}