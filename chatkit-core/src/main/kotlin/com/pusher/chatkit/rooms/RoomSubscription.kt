package com.pusher.chatkit.rooms

import com.google.gson.JsonElement
import com.pusher.chatkit.*
import com.pusher.chatkit.rooms.RoomSubscriptionEvent.*
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.users.User
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.wait
import com.pusher.util.Result
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Error
import elements.Errors
import elements.Subscription
import java.net.URL

internal class RoomSubscription(
    private val roomId: Int,
    userId: String,
    private val consumeEvent: RoomSubscriptionConsumer,
    private val chatManager: ChatManager,
    messageLimit: Int
) : Subscription {

    private var active = true

    private val subscription = chatManager.apiInstance.subscribeResuming(
        path = "/rooms/$roomId?user_id=$userId&message_limit=$messageLimit",
        tokenProvider = chatManager.tokenProvider,
        listeners = SubscriptionListeners<ChatEvent>(
            onEvent = { it.body.toRoomEvent().let(consumeEvent) },
            onError = { consumeEvent(ErrorOccurred(it)) }
        ),
        messageParser = { it.parseAs() }
    )


    private val cursorSubscription = chatManager.cursorService.subscribeToRoomCursors(roomId) { event ->
        when(event) {
            is CursorSubscriptionEvent.OnCursorSet -> consumeEvent(RoomSubscriptionEvent.NewReadCursor(event.cursor))
        }
    }

    init {
        check(messageLimit > 0) { "messageLimit should be greater than 0" }
        chatManager.observerEvents { if (active) it.consume() }
    }

    private fun ChatManagerEvent.consume() = when {
        this is ChatManagerEvent.UserStartedTyping && room.id == roomId -> UserStartedTyping(user)
        this is ChatManagerEvent.UserStoppedTyping && room.id == roomId -> UserStoppedTyping(user)
        this is ChatManagerEvent.UserJoinedRoom && room.id == roomId -> UserJoined(user)
        this is ChatManagerEvent.UserLeftRoom && room.id == roomId -> UserLeft(user)
        this is ChatManagerEvent.UserCameOnline && user.isInRoom() -> UserCameOnline(user)
        this is ChatManagerEvent.UserWentOffline && user.isInRoom() -> UserWentOffline(user)
        this is ChatManagerEvent.RoomUpdated && room.id == roomId -> RoomUpdated(room)
        this is ChatManagerEvent.RoomDeleted && roomId == this.roomId -> RoomDeleted(roomId)
        else -> null
    }?.let(consumeEvent)

    private fun User.isInRoom() = chatManager.roomService.fetchRoomBy(id, roomId).mapResult { true }.wait().recover { false }

    private fun ChatEvent.toRoomEvent() : RoomSubscriptionEvent = when(eventName) {
        "new_message" -> data.toNewMessage()
        else -> ErrorOccurred(Errors.other("Wrong event type: $eventName")).asSuccess<RoomSubscriptionEvent, Error>()
    }.recover { error -> ErrorOccurred(error) }

    private fun JsonElement.toNewMessage(): Result<RoomSubscriptionEvent, Error> = parseAs<Message>()
        .map { message ->
            if (message.attachment != null) {
                val attachmentURL = URL(message.attachment.link)
                val queryParamsMap: MutableMap<String, String> = mutableMapOf()
                attachmentURL.query.split("&").forEach { pair ->
                    val splitPair = pair.split("=")
                    if (splitPair.count() == 2) {
                        queryParamsMap[splitPair[0]] = splitPair[1]
                    }
                }
                if (queryParamsMap["chatkit_link"] == "true") {
                    message.attachment.fetchRequired = true
                }
            }
            NewMessage(message)
        }

    override fun unsubscribe() {
        active = false
        subscription.unsubscribe()
        cursorSubscription.unsubscribe()
    }

}

