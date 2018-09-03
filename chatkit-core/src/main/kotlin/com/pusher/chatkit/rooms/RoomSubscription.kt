package com.pusher.chatkit.rooms

import com.google.gson.JsonElement
import com.pusher.chatkit.*
import com.pusher.chatkit.rooms.RoomSubscriptionEvent.*
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.util.parseAs
import com.pusher.chatkit.users.User
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.Futures
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
    private val consumeEvent: RoomSubscriptionConsumer,
    private val chatManager: ChatManager,
    private val messageLimit: Int
) : ChatkitSubscription {
    private var active = true
    private val logger = chatManager.dependencies.logger
    private lateinit var roomSubscription: Subscription
    private lateinit var cursorSubscription: Subscription
    private lateinit var membershipSubscription: Subscription

    init {
        check(messageLimit >= 0) { "messageLimit must be positive" }
        chatManager.observerEvents { if (active) it.consume() }
    }

    override fun connect(): ChatkitSubscription {
        val deferredRoomSubscription = Futures.schedule {
            ResolvableSubscription(
                path = "/rooms/$roomId?&message_limit=$messageLimit",
                listeners = SubscriptionListeners<ChatEvent>(
                    onOpen = { headers ->
                        logger.verbose("[Room] On open $headers")
                    },
                    onEvent = { it.body.toRoomEvent().let(consumeEvent) },
                    onError = { consumeEvent(ErrorOccurred(it)) }
                ),
                chatManager = chatManager,
                messageParser = { it.parseAs() }
            ).connect()
        }

        val deferredMembershipSubscription = Futures.schedule {
            chatManager.membershipService.subscribe(roomId) { event ->
                when (event) {
                    is ChatManagerEvent.UserJoinedRoom -> consumeEvent(RoomSubscriptionEvent.UserJoined(event.user))
                    is ChatManagerEvent.UserLeftRoom -> consumeEvent(RoomSubscriptionEvent.UserLeft(event.user))
                }
            }
        }

        val deferredCursorSubscription = Futures.schedule {
            chatManager.cursorService.subscribeForRoom(roomId) { event ->
                when (event) {
                    is CursorSubscriptionEvent.OnCursorSet -> consumeEvent(RoomSubscriptionEvent.NewReadCursor(event.cursor))
                    is CursorSubscriptionEvent.InitialState -> consumeEvent(RoomSubscriptionEvent.InitialReadCursors(event.cursors))
                }
            }
        }

        roomSubscription = deferredRoomSubscription.wait()
        membershipSubscription = deferredMembershipSubscription.wait()
        cursorSubscription = deferredCursorSubscription.wait()

        return this
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
                val queryParamsMap: Map<String, String> = (URL(message.attachment.link).query?.split("&") ?: emptyList())
                    .mapNotNull { it.split("=").takeIf { it.size == 2 } }
                    .map { (key, value) -> key to value }
                    .toMap()
                if (queryParamsMap["chatkit_link"] == "true") {
                    message.attachment.fetchRequired = true
                }
            }
            chatManager.userService.fetchUserBy(message.userId).wait().fold(
                    onFailure = {},
                    onSuccess = { message.user = it }
            )
            NewMessage(message)

        }

    override fun unsubscribe() {
        active = false
        cursorSubscription.unsubscribe()
        membershipSubscription.unsubscribe()
        roomSubscription.unsubscribe()
    }

}

