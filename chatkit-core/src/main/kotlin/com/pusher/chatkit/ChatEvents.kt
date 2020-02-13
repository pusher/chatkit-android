package com.pusher.chatkit

import com.pusher.chatkit.ChatEvent.AddedToRoom
import com.pusher.chatkit.ChatEvent.CurrentUserReceived
import com.pusher.chatkit.ChatEvent.ErrorOccurred
import com.pusher.chatkit.ChatEvent.NewReadCursor
import com.pusher.chatkit.ChatEvent.NoEvent
import com.pusher.chatkit.ChatEvent.PresenceChange
import com.pusher.chatkit.ChatEvent.RemovedFromRoom
import com.pusher.chatkit.ChatEvent.RoomDeleted
import com.pusher.chatkit.ChatEvent.RoomUpdated
import com.pusher.chatkit.ChatEvent.UserJoinedRoom
import com.pusher.chatkit.ChatEvent.UserLeftRoom
import com.pusher.chatkit.ChatEvent.UserStartedTyping
import com.pusher.chatkit.ChatEvent.UserStoppedTyping
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.users.User
import elements.Error

/**
 * Used along with [SynchronousChatManager] to observe global changes in the chat.
 */
data class ChatListeners @JvmOverloads constructor(
    val onCurrentUserReceived: (SynchronousCurrentUser) -> Unit = {},
    val onUserStartedTyping: (User, Room) -> Unit = { _, _ -> },
    val onUserStoppedTyping: (User, Room) -> Unit = { _, _ -> },
    val onUserJoinedRoom: (User, Room) -> Unit = { _, _ -> },
    val onUserLeftRoom: (User, Room) -> Unit = { _, _ -> },
    val onPresenceChanged: (User, Presence, Presence) -> Unit = { _, _, _ -> },
    val onAddedToRoom: (Room) -> Unit = { },
    val onRemovedFromRoom: (String) -> Unit = { },
    val onRoomUpdated: (Room) -> Unit = { },
    val onRoomDeleted: (String) -> Unit = { },
    val onNewReadCursor: (Cursor) -> Unit = { },
    val onErrorOccurred: (Error) -> Unit = { }
)

/**
 * Used to consume instances of [ChatEvent]
 */
typealias ChatManagerEventConsumer = (ChatEvent) -> Unit

/**
 * Transforms [ChatListeners] to [ChatManagerEventConsumer]
 */
internal fun ChatListeners.toCallback(): ChatManagerEventConsumer = { event ->
    when (event) {
        is CurrentUserReceived -> onCurrentUserReceived(event.currentUser)
        is UserStartedTyping -> onUserStartedTyping(event.user, event.room)
        is UserStoppedTyping -> onUserStoppedTyping(event.user, event.room)
        is UserJoinedRoom -> onUserJoinedRoom(event.user, event.room)
        is UserLeftRoom -> onUserLeftRoom(event.user, event.room)
        is PresenceChange -> onPresenceChanged(event.user, event.currentState, event.prevState)
        is AddedToRoom -> onAddedToRoom(event.room)
        is RemovedFromRoom -> onRemovedFromRoom(event.roomId)
        is RoomUpdated -> onRoomUpdated(event.room)
        is RoomDeleted -> onRoomDeleted(event.roomId)
        is ErrorOccurred -> onErrorOccurred(event.error)
        is NewReadCursor -> onNewReadCursor(event.cursor)
        is NoEvent -> Unit // Ignore
    }
}

/**
 * Same as [ChatListeners] but using events instead of individual listeners.
 */
sealed class ChatEvent {
    data class CurrentUserReceived internal constructor(val currentUser: SynchronousCurrentUser) : ChatEvent()
    data class UserStartedTyping internal constructor(val user: User, val room: Room) : ChatEvent()
    data class UserStoppedTyping internal constructor(val user: User, val room: Room) : ChatEvent()
    data class UserJoinedRoom internal constructor(val user: User, val room: Room) : ChatEvent()
    data class UserLeftRoom internal constructor(val user: User, val room: Room) : ChatEvent()
    data class PresenceChange internal constructor(val user: User, val currentState: Presence, val prevState: Presence) : ChatEvent()
    data class AddedToRoom internal constructor(val room: Room) : ChatEvent()
    data class RemovedFromRoom internal constructor(val roomId: String) : ChatEvent()
    data class RoomUpdated internal constructor(val room: Room) : ChatEvent()
    data class RoomDeleted internal constructor(val roomId: String) : ChatEvent()
    data class ErrorOccurred internal constructor(val error: elements.Error) : ChatEvent()
    data class NewReadCursor internal constructor(val cursor: Cursor) : ChatEvent()
    object NoEvent : ChatEvent()
}
