package com.pusher.chatkit.rooms

import com.pusher.chatkit.messages.Message
import elements.Error


typealias RoomSubscriptionConsumer = (RoomSubscriptionEvent) -> Unit

/**
 * Events which may be received on a backend room subscription. This is what we will parse from the
 * backend.
 *
 * Contrast with RoomEvent, the user facing events which are related to a room.
 */
sealed class RoomSubscriptionEvent {
    data class NewMessage(val message: Message) : RoomSubscriptionEvent()
    data class UserIsTyping(val userId: String) : RoomSubscriptionEvent()
    data class ErrorOccurred(val error: Error) : RoomSubscriptionEvent()
}
