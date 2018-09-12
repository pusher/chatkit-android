package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatManagerEvent
import com.pusher.chatkit.ChatManagerEventConsumer
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.memberships.MembershipSubscriptionEventParser
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.User
import com.pusher.chatkit.users.UserService
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import elements.Subscription
import java.util.concurrent.Future


class RoomSubscriptionGroup(
        messageLimit: Int,
        roomId: Int,
        private val userService: UserService,
        cursorService: CursorService,
        private val globalEventConsumers: MutableList<ChatManagerEventConsumer>,
        client: PlatformClient,
        logger: Logger,
        private val consumers: List<RoomConsumer>
): ChatkitSubscription {
    init {
        check(messageLimit >= 0) { "messageLimit must be greater than or equal to 0" }

        globalEventConsumers += this::consumeEvent
    }

    private val roomSubscription = ResolvableSubscription(
            client = client,
            path = "/rooms/$roomId?&message_limit=$messageLimit",
            listeners = SubscriptionListeners(
                    onEvent = { consumeEvent(it.body) },
                    onError = { consumeEvent(RoomSubscriptionEvent.ErrorOccurred(it)) }
            ),
            messageParser = RoomSubscriptionEventParser,
            description = "Room $roomId",
            logger = logger
    )

    private val membershipSubscription = ResolvableSubscription(
            client = client,
            path = "/rooms/$roomId/memberships",
            listeners = SubscriptionListeners(
                    onEvent = { event -> consumeEvent(event.body) },
                    onError = { error -> consumeEvent(MembershipSubscriptionEvent.ErrorOccurred(error)) }
            ),
            messageParser = MembershipSubscriptionEventParser,
            logger = logger,
            description = "Memberships room $roomId",
            resolveOnFirstEvent = true
    )

    private val cursorsSubscription = cursorService.subscribeForRoom(
            roomId,
            this::consumeEvent
    )

    private val typingTimers = HashMap<String, Future<Unit>>()

    override fun connect(): Subscription {
        roomSubscription.await()
        membershipSubscription.await()
        cursorsSubscription.await()

        return this
    }

    override fun unsubscribe() {
        roomSubscription.unsubscribe()
        membershipSubscription.unsubscribe()
        cursorsSubscription.unsubscribe()

        globalEventConsumers -= this::consumeEvent
    }

    private fun forwardEvent(event: RoomEvent) {
        consumers.forEach { consumer ->
            consumer(event)
        }
    }

    private fun consumeEvent(event: MembershipSubscriptionEvent) {
        val events = when (event) {
            is MembershipSubscriptionEvent.UserJoined -> listOf(
                    userService.fetchUserBy(event.userId).map { user ->
                        RoomEvent.UserJoined(user) as RoomEvent
                    }.recover { RoomEvent.ErrorOccurred(it) }
                )
            is MembershipSubscriptionEvent.UserLeft -> listOf(
                    userService.fetchUserBy(event.userId).map { user ->
                        RoomEvent.UserLeft(user) as RoomEvent
                    }.recover { RoomEvent.ErrorOccurred(it) }
                )
            is MembershipSubscriptionEvent.InitialState ->
                // TODO we shouldn't fetch each user one at a time
                event.userIds.map { userId ->
                    userService.fetchUserBy(userId).map { user ->
                        RoomEvent.UserJoined(user) as RoomEvent
                    }.recover { RoomEvent.ErrorOccurred(it) }
                }
            is MembershipSubscriptionEvent.ErrorOccurred ->
                listOf(RoomEvent.ErrorOccurred(event.error))
        }

        events.forEach(::forwardEvent)
    }

    private fun consumeEvent(event: CursorSubscriptionEvent) {
        forwardEvent(
                when (event) {
                    is CursorSubscriptionEvent.OnCursorSet -> RoomEvent.NewReadCursor(event.cursor)
                    is CursorSubscriptionEvent.InitialState -> RoomEvent.InitialReadCursors(event.cursors)
                    is CursorSubscriptionEvent.OnError -> RoomEvent.ErrorOccurred(event.error)
                }
        )
    }

    private fun consumeEvent(event: RoomSubscriptionEvent) {
        forwardEvent(
                when (event) {
                    is RoomSubscriptionEvent.UserIsTyping -> {
                        userService.fetchUserBy(event.userId).map { user ->
                            if (scheduleStopTypingEvent(user)) {
                                RoomEvent.UserStartedTyping(user)
                            } else {
                                RoomEvent.NoEvent
                            }
                        }.recover {
                            RoomEvent.ErrorOccurred(it)
                        }
                    }
                    is RoomSubscriptionEvent.NewMessage ->
                        userService.fetchUserBy(event.message.userId).map { user ->
                            event.message.user = user
                            RoomEvent.NewMessage(event.message) as RoomEvent
                        }.recover {
                            RoomEvent.ErrorOccurred(it)
                        }
                    is RoomSubscriptionEvent.ErrorOccurred ->
                        RoomEvent.ErrorOccurred(event.error)
                }
        )
    }

    private fun consumeEvent(event: ChatManagerEvent) {
        forwardEvent(
                // This function must map events which we wish to report at room scope that
                // are not received at room scope from the backend.
                // Be careful, if you map an event where which originated here, you will create
                // an infinite loop consuming that event.
                when (event) {
                    is ChatManagerEvent.RoomUpdated ->
                        RoomEvent.RoomUpdated(event.room)
                    is ChatManagerEvent.RoomDeleted ->
                        RoomEvent.RoomDeleted(event.roomId)
                    is ChatManagerEvent.UserCameOnline ->
                        RoomEvent.UserCameOnline(event.user)
                    is ChatManagerEvent.UserWentOffline ->
                        RoomEvent.UserWentOffline(event.user)
                    else ->
                        RoomEvent.NoEvent
                }
        )
    }

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
                forwardEvent(RoomEvent.UserStoppedTyping(user))
            }

            return new
        }
    }
}