package com.pusher.chatkit.rooms

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.memberships.MembershipSubscription
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.users.User
import com.pusher.chatkit.users.UserService
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.platform.network.waitOr
import com.pusher.util.asSuccess
import com.pusher.util.mapResult
import elements.Errors
import java.util.concurrent.Future


class RoomSubscriptionGroup(
        messageLimit: Int,
        roomId: Int,
        private val userService: UserService,
        cursorService: CursorService,
        client: PlatformClient,
        logger: Logger,
        private val consumers: List<RoomConsumer>
        ): ChatkitSubscription {
    private val roomSubscription = RoomSubscription(
            roomId,
            this::consumeEvent,
            client,
            messageLimit,
            logger
    )

    private val membershipSubscription = MembershipSubscription(
            roomId,
            client,
            this::consumeEvent,
            logger
    )

    private val cursorsSubscription = cursorService.subscribeForRoom(
            roomId,
            this::consumeEvent
    )

    private val typingTimers = HashMap<String, Future<Unit>>()

    override fun unsubscribe() {
        roomSubscription.unsubscribe()
        membershipSubscription.unsubscribe()
        cursorsSubscription.unsubscribe()
    }

    override fun connect(): ChatkitSubscription {
        // TODO these should be done in parallel
        // TODO the cursors service already called connect on the Cursors Subscription!
        roomSubscription.connect()
        membershipSubscription.connect()

        return this
    }

    private fun forwardEvent(event: RoomEvent) {
        consumers.forEach { consumer ->
            consumer(event)
        }
    }

    private fun consumeEvent(event: MembershipSubscriptionEvent) {
        val events = when (event) {
            is MembershipSubscriptionEvent.UserJoined -> listOf(
                    userService.fetchUserBy(event.userId).mapResult { user ->
                        RoomEvent.UserJoined(user) as RoomEvent
                    }.waitOr { RoomEvent.ErrorOccurred(Errors.other(it)).asSuccess()
                    }.recover { RoomEvent.ErrorOccurred(it) }
                )
            is MembershipSubscriptionEvent.UserLeft -> listOf(
                    userService.fetchUserBy(event.userId).mapResult { user ->
                        RoomEvent.UserLeft(user) as RoomEvent
                    }.waitOr { RoomEvent.ErrorOccurred(Errors.other(it)).asSuccess()
                    }.recover { RoomEvent.ErrorOccurred(it) }
                )
            is MembershipSubscriptionEvent.InitialState ->
                // TODO we shouldn't fetch each user one at a time
                event.userIds.map { userId ->
                    userService.fetchUserBy(userId).mapResult { user ->
                        RoomEvent.UserJoined(user) as RoomEvent
                    }.waitOr { RoomEvent.ErrorOccurred(Errors.other(it)).asSuccess()
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
                    is CursorSubscriptionEvent.NoEvent -> RoomEvent.NoEvent
                }
        )
    }

    private fun consumeEvent(event: RoomSubscriptionEvent) {
        forwardEvent(
                when (event) {
                    is RoomSubscriptionEvent.UserIsTyping -> {
                        userService.fetchUserBy(event.userId).mapResult { user ->
                            if (scheduleStopTypingEvent(user)) {
                                RoomEvent.UserStartedTyping(user)
                            } else {
                                RoomEvent.NoEvent
                            }
                        }.waitOr {
                            RoomEvent.ErrorOccurred(Errors.other(it)).asSuccess()
                        }.recover {
                            RoomEvent.ErrorOccurred(it)
                        }
                    }
                    is RoomSubscriptionEvent.NewMessage ->
                        RoomEvent.NewMessage(event.message)
                    is RoomSubscriptionEvent.ErrorOccurred ->
                        RoomEvent.ErrorOccurred(event.error)
                    is RoomSubscriptionEvent.NoEvent ->
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