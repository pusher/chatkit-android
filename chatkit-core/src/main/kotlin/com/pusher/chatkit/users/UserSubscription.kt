package com.pusher.chatkit.users

import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.ChatManagerEvent.CurrentUserRemovedFromRoom
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.UserSubscriptionEvent.*
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.platform.network.wait
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import elements.emptyHeaders
import java.lang.Thread.sleep
import java.util.concurrent.Future

private const val USERS_PATH = "users"

internal class UserSubscription(
    private val client: PlatformClient,
    private val roomService: RoomService,
    private val consumeEvent: (UserSubscriptionEvent) -> Unit,
    private val logger: Logger
) : ChatkitSubscription {
    private var headers: Headers = emptyHeaders()

    private lateinit var underlyingSubscription: Subscription

    override fun connect(): ChatkitSubscription {
        val deferredUserSubscription = Futures.schedule {
            ResolvableSubscription(
                client = client,
                path = USERS_PATH,
                listeners = SubscriptionListeners(
                    onOpen = { headers ->
                        logger.verbose("[User] OnOpen $headers")
                        this@UserSubscription.headers = headers
                    },
                    onEvent = { event: SubscriptionEvent<UserSubscriptionEvent> ->
                        event.body
                            .applySideEffects()
                            .also(consumeEvent)
                            .also { logger.verbose("[User] Event received $it") }
                    },
                    onError = { error -> consumeEvent(UserSubscriptionEvent.ErrorOccurred(error)) },
                    onSubscribe = { logger.verbose("[User] Subscription established.") },
                    onRetrying = { logger.verbose("[User] Subscription lost. Trying again.") },
                    onEnd = { error -> logger.verbose("[User] Subscription ended with: $error") }
                ),
                messageParser = UserSubscriptionEventParser,
                resolveOnFirstEvent = true
            ).connect()
        }

        underlyingSubscription = deferredUserSubscription.wait()

        return this
    }

    override fun unsubscribe() {
        underlyingSubscription.unsubscribe()
    }

    private fun UserSubscriptionEvent.applySideEffects(): UserSubscriptionEvent = this.apply {
        when (this) {
            is InitialState -> {
                for (event in updateExistingRooms(rooms)) {
                    consumeEvent(event)
                }
                roomService.roomStore += rooms
            }
            is AddedToRoomEvent -> roomService.roomStore += room
            is RoomUpdatedEvent -> roomService.roomStore += room
            is RoomDeletedEvent -> roomService.roomStore -= roomId
            is RemovedFromRoomEvent -> roomService.roomStore -= roomId
            is LeftRoomEvent -> roomService.roomStore[roomId]?.removeUser(userId)
            is JoinedRoomEvent -> roomService.roomStore[roomId]?.addUser(userId)
            is StartedTyping -> typingTimers
                .firstOrNull { it.userId == userId && it.roomId == roomId }
                ?: TypingTimer(userId, roomId).also { typingTimers += it }
                    .triggerTyping()
        }
    }

    private val typingTimers = mutableListOf<TypingTimer>()

    inner class TypingTimer(val userId: String, val roomId: Int) {

        private var job: Future<Unit>? = null

        fun triggerTyping() {
            if (job.isActive) job?.cancel()
            job = scheduleStopTyping()
        }

        private fun scheduleStopTyping(): Future<Unit> = Futures.schedule {
            sleep(1_500)
            val event = UserSubscriptionEvent.StoppedTyping(userId, roomId)
                .toChatManagerEvent()
                .wait()
                .recover { ErrorOccurred(it) }
            consumeEvent(event)
        }

        private val <A> Future<A>?.isActive
            get() = this != null && !isDone && !isCancelled

    }

    private fun updateExistingRooms(roomsForConnection: List<Room>): List<ChatManagerEvent> {
        return (roomService.roomStore.toList() - roomsForConnection)
            .map { CurrentUserRemovedFromRoom(it.id) }
    }
}