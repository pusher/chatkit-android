package com.pusher.chatkit

import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.users.User
import elements.Error

/**
 * Used along with [ChatManager] to observe global changes in the chat.
 */
data class ChatManagerListeners @JvmOverloads constructor(
    val onCurrentUserReceived: (CurrentUser) -> Unit = {},
    val onUserStartedTyping: (User, Room) -> Unit = { _, _ -> },
    val onUserStoppedTyping: (User, Room) -> Unit = { _, _ -> },
    val onUserJoinedRoom: (User, Room) -> Unit = { _, _ -> },
    val onUserLeftRoom: (User, Room) -> Unit = { _, _ -> },
    val onUserCameOnline: (User) -> Unit = { },
    val onUserWentOffline: (User) -> Unit = { },
    val onCurrentUserAddedToRoom: (Room) -> Unit = { },
    val onCurrentUserRemovedFromRoom: (Int) -> Unit = { },
    val onRoomUpdated: (Room) -> Unit = { },
    val onRoomDeleted: (Int) -> Unit = { },
    val onNewReadCursor: (Cursor) -> Unit = { },
    val onErrorOccurred: (Error) -> Unit = { }
)

/**
 * Used to consume instances of [ChatManagerEvent]
 */
typealias ChatManagerEventConsumer = (ChatManagerEvent) -> Unit

/**
 * Transforms [ChatManagerListeners] to [ChatManagerEventConsumer]
 */
internal fun ChatManagerListeners.toCallback(): ChatManagerEventConsumer = { event ->
    when (event) {
        is CurrentUserReceived -> onCurrentUserReceived(event.currentUser)
        is UserStartedTyping -> onUserStartedTyping(event.user, event.room)
        is UserStoppedTyping -> onUserStoppedTyping(event.user, event.room)
        is UserJoinedRoom -> onUserJoinedRoom(event.user, event.room)
        is UserLeftRoom -> onUserLeftRoom(event.user, event.room)
        is UserCameOnline -> onUserCameOnline(event.user)
        is UserWentOffline -> onUserWentOffline(event.user)
        is CurrentUserAddedToRoom -> onCurrentUserAddedToRoom(event.room)
        is CurrentUserRemovedFromRoom -> onCurrentUserRemovedFromRoom(event.roomId)
        is RoomUpdated -> onRoomUpdated(event.room)
        is RoomDeleted -> onRoomDeleted(event.roomId)
        is ErrorOccurred -> onErrorOccurred(event.error)
        is NewReadCursor -> onNewReadCursor(event.cursor)
        is NoEvent -> Unit // Ignore
    }
}

/**
 * Same as [ChatManagerListeners] but using events instead of individual listeners.
 */
sealed class ChatManagerEvent {
    data class CurrentUserReceived internal constructor(val currentUser: CurrentUser) : ChatManagerEvent()
    data class UserStartedTyping internal constructor(val user: User, val room: Room) : ChatManagerEvent()
    data class UserStoppedTyping internal constructor(val user: User, val room: Room) : ChatManagerEvent()
    data class UserJoinedRoom internal constructor(val user: User, val room: Room) : ChatManagerEvent()
    data class UserLeftRoom internal constructor(val user: User, val room: Room) : ChatManagerEvent()
    data class UserCameOnline internal constructor(val user: User) : ChatManagerEvent()
    data class UserWentOffline internal constructor(val user: User) : ChatManagerEvent()
    data class CurrentUserAddedToRoom internal constructor(val room: Room) : ChatManagerEvent()
    data class CurrentUserRemovedFromRoom internal constructor(val roomId: Int) : ChatManagerEvent()
    data class RoomUpdated internal constructor(val room: Room) : ChatManagerEvent()
    data class RoomDeleted internal constructor(val roomId: Int) : ChatManagerEvent()
    data class ErrorOccurred internal constructor(val error: elements.Error) : ChatManagerEvent()
    data class NewReadCursor internal constructor(val cursor: Cursor) : ChatManagerEvent()
    object NoEvent : ChatManagerEvent()
}
