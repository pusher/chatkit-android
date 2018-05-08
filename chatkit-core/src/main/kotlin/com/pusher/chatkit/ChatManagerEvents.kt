package com.pusher.chatkit

import com.pusher.chatkit.ChatManagerEvent.*
import elements.Error

/**
 * Used along with [ChatManager] to observe global changes in the chat.
 */
data class ChatManagerListeners @JvmOverloads constructor(
    val onCurrentUserReceived: (CurrentUser) -> Unit = {},
    val onUserStartedTyping: (User) -> Unit = { },
    val onUserStoppedTyping: (User) -> Unit = { },
    val onUserJoinedRoom: (User, Room) -> Unit = { _, _ -> },
    val onUserLeftRoom: (User, Room) -> Unit = { _, _ -> },
    val onUserCameOnline: (User) -> Unit = { },
    val onUserWentOffline: (User) -> Unit = { },
    val onUsersUpdated: () -> Unit = { },
    val onCurrentUserAddedToRoom: (Room) -> Unit = { },
    val onCurrentUserRemovedFromRoom: (Int) -> Unit = { },
    val onRoomUpdated: (Room) -> Unit = { },
    val onRoomDeleted: (Int) -> Unit = { },
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
        is UserStartedTyping -> onUserStartedTyping(event.user)
        is UserStoppedTyping -> onUserStoppedTyping(event.user)
        is UserJoinedRoom -> onUserJoinedRoom(event.user, event.room)
        is UserLeftRoom -> onUserLeftRoom(event.user, event.room)
        is UserCameOnline -> onUserCameOnline(event.user)
        is UserWentOffline -> onUserWentOffline(event.user)
        is UsersUpdated -> onUsersUpdated()
        is CurrentUserAddedToRoom -> onCurrentUserAddedToRoom(event.room)
        is CurrentUserRemovedFromRoom -> onCurrentUserRemovedFromRoom(event.roomId)
        is RoomUpdated -> onRoomUpdated(event.room)
        is RoomDeleted -> onRoomDeleted(event.roomId)
        is ErrorOccurred -> onErrorOccurred(event.error)
        is NoEvent -> Unit // Ignore
    }
}

/**
 * Same as [ChatManagerListeners] but using events instead of individual listeners.
 */
sealed class ChatManagerEvent {

    /**
     * @see [ChatManagerListeners.onCurrentUserReceived]
     */
    data class CurrentUserReceived internal constructor(val currentUser: CurrentUser) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onUserStartedTyping]
     */
    data class UserStartedTyping internal constructor(val user: User) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onUserStoppedTyping]
     */
    data class UserStoppedTyping internal constructor(val user: User) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onUserJoinedRoom]
     */
    data class UserJoinedRoom internal constructor(val user: User, val room: Room) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onUserLeftRoom]
     */
    data class UserLeftRoom internal constructor(val user: User, val room: Room) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onUserCameOnline]]
     */
    data class UserCameOnline internal constructor(val user: User) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onUserWentOffline]]
     */
    data class UserWentOffline internal constructor(val user: User) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onUsersUpdated]]
     */
    object UsersUpdated : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onCurrentUserAddedToRoom]
     */
    data class CurrentUserAddedToRoom internal constructor(val room: Room) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onCurrentUserRemovedFromRoom]
     */
    data class CurrentUserRemovedFromRoom internal constructor(val roomId: Int) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onRoomUpdated]
     */
    data class RoomUpdated internal constructor(val room: Room) : ChatManagerEvent()

    /**
     * @see [ChatManagerListeners.onRoomDeleted]
     */
    data class RoomDeleted internal constructor(val roomId: Int) : ChatManagerEvent()

    /**
     * Same as [ChatManagerListeners.onErrorOccurred]
     */
    data class ErrorOccurred internal constructor(val error: elements.Error) : ChatManagerEvent()

    /**
     * Used for control events, it can be ignored.
     */
    object NoEvent : ChatManagerEvent()

}
