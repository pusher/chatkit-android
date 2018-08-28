package com.pusher.chatkit.rooms

import com.pusher.chatkit.*
import com.pusher.chatkit.rooms.RoomSubscriptionEvent.*
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.User
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.Error
import elements.Errors
import elements.Subscription
import elements.SubscriptionEvent
import kotlinx.coroutines.experimental.async
import java.net.URL
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

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

    override suspend fun connect(): ChatkitSubscription {
        val deferredRoomSubscription = async {
            ResolvableSubscription(
                path = "/rooms/$roomId?&message_limit=$messageLimit",
                listeners = SubscriptionListeners(
                    onOpen = { headers ->
                        logger.verbose("[Room] On open $headers")
                    },
                    onEvent = { event: SubscriptionEvent<RoomSubscriptionEvent> ->
                        event.body
                            .applySideEffects()
                            .also(consumeEvent)
                            .also { logger.verbose("[Room] Event received $it") }
                            .toChatManagerEvent()
                            .waitOr(Wait.For(10, TimeUnit.SECONDS)) { ChatManagerEvent.ErrorOccurred(Errors.other(it)).asSuccess() }
                            .recover { ChatManagerEvent.ErrorOccurred(it) }
                            .consume()
                    },
                    onError = { consumeEvent(ErrorOccurred(it)) }
                ),
                chatManager = chatManager,
                messageParser = RoomSubscriptionEventParser
            ).connect()
        }

        val deferredMembershipSubscription = async {
            chatManager.membershipService.subscribe(roomId) { event ->
                when (event) {
                    is ChatManagerEvent.UserJoinedRoom -> consumeEvent(RoomSubscriptionEvent.UserJoined(event.user))
                    is ChatManagerEvent.UserLeftRoom -> consumeEvent(RoomSubscriptionEvent.UserLeft(event.user))
                }
            }
        }

        val deferredCursorSubscription = async {
            chatManager.cursorService.subscribeForRoom(roomId) { event ->
                when (event) {
                    is CursorSubscriptionEvent.OnCursorSet -> consumeEvent(RoomSubscriptionEvent.NewReadCursor(event.cursor))
                    is CursorSubscriptionEvent.InitialState -> consumeEvent(RoomSubscriptionEvent.InitialReadCursors(event.cursors))
                }
            }
        }

        roomSubscription = deferredRoomSubscription.await()
        membershipSubscription = deferredMembershipSubscription.await()
        cursorSubscription = deferredCursorSubscription.await()

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

    private fun RoomSubscriptionEvent.applySideEffects(): RoomSubscriptionEvent {
        return when (this) {
            is NewMessage -> {
                if (message.attachment != null) {
                    val queryParamsMap: Map<String, String> = (URL(message.attachment.link).query?.split("&")
                        ?: emptyList())
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
            is UserIsTyping -> {
                typingTimers
                    .firstOrNull { it.userId == userId && it.roomId == roomId }
                    ?: TypingTimer(userId, roomId).also { typingTimers += it }
                        .triggerTyping()
                val maybeUser = chatManager.userService.fetchUserBy(userId).get()
                when (maybeUser) {
                    is Result.Success -> UserStartedTyping(maybeUser.value)
                    is Result.Failure -> ErrorOccurred(maybeUser.error)
                }
            }
            else -> this
        }
    }

    private fun ChatManagerEvent.toFutureSuccess() = asSuccess<ChatManagerEvent, Error>().toFuture()

    private fun RoomSubscriptionEvent.toChatManagerEvent(): Future<Result<ChatManagerEvent, Error>> = when (this) {
        is UserStartedTyping -> chatManager.roomService.fetchRoomBy(user.id, roomId).flatMapResult { room ->
            ChatManagerEvent.UserStartedTyping(user, room).asSuccess<ChatManagerEvent, Error>()
        }
        is UserStoppedTyping -> chatManager.roomService.fetchRoomBy(user.id, roomId).flatMapResult { room ->
            ChatManagerEvent.UserStoppedTyping(user, room).asSuccess<ChatManagerEvent, Error>()
        }
        else -> ChatManagerEvent.NoEvent.toFutureSuccess()
    }

    override fun unsubscribe() {
        active = false
        cursorSubscription.unsubscribe()
        membershipSubscription.unsubscribe()
        roomSubscription.unsubscribe()
    }

    private val typingTimers = mutableListOf<TypingTimer>()
    inner class TypingTimer(val userId: String, val roomId: Int) {
        private var job: Future<Unit>? = null

        fun triggerTyping() {
            if (job.isActive) job?.cancel()
            job = scheduleStopTyping()
        }

        private fun scheduleStopTyping(): Future<Unit> = Futures.schedule {
            Thread.sleep(1_500)
            chatManager.userService.fetchUserBy(userId).mapResult { user ->
                consumeEvent(RoomSubscriptionEvent.UserStoppedTyping(user))
            }.wait().recover { RoomSubscriptionEvent.ErrorOccurred(it) }
        }

        private val <A> Future<A>?.isActive
            get() = this != null && !isDone && !isCancelled

    }

}

