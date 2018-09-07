package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.rooms.RoomSubscriptionEvent.*
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.User
import com.pusher.chatkit.users.UserService
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.platform.network.wait
import com.pusher.platform.network.waitOr
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal class RoomSubscription(
    private val roomId: Int,
    private val consumeEvent: RoomSubscriptionConsumer,
    private val client: PlatformClient,
    private val chatManager: ChatManager,
    private val userService: UserService,
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
                client = client,
                path = "/rooms/$roomId?&message_limit=$messageLimit",
                listeners = SubscriptionListeners(
                    onOpen = { logger.verbose("[Room $roomId] On open triggered") },
                    onEvent = {
                        logger.verbose("[Room $roomId] received event: ${it.body}")
                        consumeEvent(transformEvent(it.body)) },
                    onError = { consumeEvent(ErrorOccurred(it)) }
                ),
                messageParser = RoomSubscriptionEventParser
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

    private fun transformEvent(event: RoomSubscriptionEvent): RoomSubscriptionEvent =
            when (event) {
                is UserIsTyping -> {
                    userService.fetchUserBy(event.userId).mapResult { user ->
                        if (scheduleStopTypingEvent(user)) {
                            RoomSubscriptionEvent.UserStartedTyping(user)
                        } else {
                            RoomSubscriptionEvent.NoEvent
                        }
                    }.waitOr {
                        RoomSubscriptionEvent.ErrorOccurred(Errors.other(it)).asSuccess()
                    }.recover {
                        RoomSubscriptionEvent.ErrorOccurred(it)
                    }
                }
                else ->
                    event
            }

    private val typingTimers = HashMap<String, Future<Unit>>()

    private fun scheduleStopTypingEvent(user: User): Boolean {
        synchronized(typingTimers) {
            val new = typingTimers[user.id] == null
            if (!new) {
                typingTimers[user.id]!!.cancel()
            }

            typingTimers[user.id] = Futures.schedule {
                Thread.sleep(1_500)
                synchronized(typingTimers) {
                    typingTimers.remove(user.id)
                }
                consumeEvent(RoomSubscriptionEvent.UserStoppedTyping(user))
            }

            return new
        }
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

    override fun unsubscribe() {
        cursorSubscription.unsubscribe()
        membershipSubscription.unsubscribe()
        roomSubscription.unsubscribe()
    }
}

