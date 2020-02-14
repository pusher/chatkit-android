package com.pusher.chatkit.rooms

import com.pusher.chatkit.messages.api.MessageBodyApiType
import elements.Error

internal typealias RoomSubscriptionConsumer = (RoomSubscriptionEvent) -> Unit

/**
 * Events which may be received on a backend room subscription. This is what we will parse from the
 * backend.
 *
 * Contrast with RoomEvent, the user facing events which are related to a room.
 */
internal sealed class RoomSubscriptionEvent {
    data class NewMultipartMessage(val message: MessageBodyApiType) : RoomSubscriptionEvent()
    data class MessageDeleted(val messageId: Int) : RoomSubscriptionEvent()
    data class UserIsTyping(val userId: String) : RoomSubscriptionEvent()
    data class ErrorOccurred(val error: Error) : RoomSubscriptionEvent()
}
