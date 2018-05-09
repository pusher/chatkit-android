package com.pusher.chatkit.rooms

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.users.User
import elements.Error

/**
 * Used in [CurrentUser] to register for room events.
 */
data class RoomSubscriptionListeners @JvmOverloads constructor(
    val onNewMessage: (Message) -> Unit = {},
    val onUserStartedTyping: (User) -> Unit = {},
    val onUserJoined: (User) -> Unit = {},
    val onUserLeft: (User) -> Unit = {},
    val onUserCameOnline: (User) -> Unit = {},
    val onUserWentOffline: (User) -> Unit = {},
    val onNewReadCursor: (Cursor) -> Unit = {},
    val onErrorOccurred: (Error) -> Unit = {}
)

/**
 * Used to consume instances of [RoomSubscriptionEvent]
 */
typealias RoomSubscriptionConsumer = (RoomSubscriptionEvent) -> Unit

/**
 * Transforms [RoomSubscriptionListeners] to [RoomSubscriptionConsumer]
 */
internal fun RoomSubscriptionListeners.toCallback(): RoomSubscriptionConsumer = { event ->
    when(event) {
        is RoomSubscriptionEvent.NewMessage -> onNewMessage(event.message)
        is RoomSubscriptionEvent.UserStartedTyping -> onUserStartedTyping(event.user)
        is RoomSubscriptionEvent.UserJoined -> onUserJoined(event.user)
        is RoomSubscriptionEvent.UserLeft -> onUserLeft(event.user)
        is RoomSubscriptionEvent.UserCameOnline -> onUserCameOnline(event.user)
        is RoomSubscriptionEvent.UserWentOffline -> onUserWentOffline(event.user)
        is RoomSubscriptionEvent.NewReadCursor -> onNewReadCursor(event.cursor)
        is RoomSubscriptionEvent.ErrorOccurred -> onErrorOccurred(event.error)
    }
}

/**
 * Same as [RoomSubscriptionListeners] but using events instead of individual listeners.
 */
sealed class RoomSubscriptionEvent {
    data class NewMessage(val message: Message) : RoomSubscriptionEvent()
    data class UserStartedTyping(val user: User) : RoomSubscriptionEvent()
    data class UserJoined(val user: User) : RoomSubscriptionEvent()
    data class UserLeft(val user: User) : RoomSubscriptionEvent()
    data class UserCameOnline(val user: User) : RoomSubscriptionEvent()
    data class UserWentOffline(val user: User) : RoomSubscriptionEvent()
    data class NewReadCursor(val cursor: Cursor) : RoomSubscriptionEvent()
    data class ErrorOccurred(val error: Error) : RoomSubscriptionEvent()
}
