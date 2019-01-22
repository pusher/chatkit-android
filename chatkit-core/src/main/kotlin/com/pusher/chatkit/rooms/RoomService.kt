package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.toJson
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import elements.Subscription
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

internal class RoomService(
        private val client: PlatformClient,
        private val userService: UserService,
        private val cursorsService: CursorService,
        private val makeGlobalConsumer: (String) -> RoomConsumer,
        private val logger: Logger
) {
    // Access synchronised on itself
    private val openSubscriptions = HashMap<String, Subscription>()
    private val roomConsumers = ConcurrentHashMap<String, RoomConsumer>()

    internal val roomStore = RoomStore()

    fun fetchRoomBy(userId: String, id: String): Result<Room, Error> =
            getLocalRoom(id)
                    .flatRecover { client.doGet("/rooms/${URLEncoder.encode(id, "UTF-8")}") }
                    .flatMap { room ->
                        when {
                            room.memberUserIds.contains(userId) -> room.asSuccess()
                            else -> noRoomMembershipError(room).asFailure<Room, Error>()
                        }
                    }

    private fun getLocalRoom(id: String): Result<Room, Error> =
            roomStore[id]
                    .orElse { Errors.other("User not found locally") }

    fun fetchUserRooms(userId: String, joinable: Boolean = false): Result<List<Room>, Error> =
            client.doGet<List<Room>>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms?joinable=$joinable")
                    .map { rooms -> rooms.also { roomStore += it } }

    fun createRoom(
            creatorId: String,
            name: String,
            isPrivate: Boolean,
            customData: CustomData?,
            userIds: List<String>
    ): Result<Room, Error> =
            RoomCreateRequest(
                    name = name,
                    private = isPrivate,
                    createdById = creatorId,
                    customData = customData,
                    userIds = userIds
            ).toJson()
                    .flatMap { body -> client.doPost<Room>("/rooms", body) }
                    .map { room ->
                        roomStore += room
                        userService.populateUserStore(room.memberUserIds)
                        room
                    }

    fun deleteRoom(roomId: String): Result<String, Error> =
            client.doDelete<Unit?>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}")
                    .map {
                        roomStore -= roomId
                        roomId
                    }

    fun leaveRoom(userId: String, roomId: String): Result<String, Error> =
            client.doPost<Unit?>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms/${URLEncoder.encode(roomId, "UTF-8")}/leave")
                    .map {
                        roomStore -= roomId
                        roomId
                    }

    fun joinRoom(userId: String, roomId: String): Result<Room, Error> =
            client.doPost<Room>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms/${URLEncoder.encode(roomId, "UTF-8")}/join")
                    .map { room ->
                        roomStore += room
                        userService.populateUserStore(room.memberUserIds)
                        room
                    }

    fun updateRoom(
            roomId: String,
            name: String? = null,
            isPrivate: Boolean? = null,
            customData: CustomData? = null
    ): Result<Unit, Error> =
            UpdateRoomRequest(name, isPrivate, customData).toJson()
                    .flatMap { body -> client.doPut<Unit>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}", body) }

    fun isSubscribedTo(roomId: String) =
            synchronized(openSubscriptions) {
                openSubscriptions[roomId] != null
            }

    fun subscribeToRoom(
            roomId: String,
            consumer: RoomConsumer,
            messageLimit: Int
    ): Subscription {
        val globalConsumer = makeGlobalConsumer(roomId)

        val emit = { event: RoomEvent ->
            consumer(event)
            globalConsumer(event)
        }

        val sub = RoomSubscriptionGroup(
                messageLimit = messageLimit,
                roomId = roomId,
                cursorService = cursorsService,
                membershipConsumer = { roomStore.applyMembershipEvent(roomId, it).map(::enrichEvent).forEach(emit) },
                roomConsumer = { emit(enrichEvent(it, emit)) },
                cursorConsumer = { emit(translateCursorEvent(it)) },
                client = client,
                logger = logger
        )
        synchronized(openSubscriptions) {
            openSubscriptions[roomId]?.unsubscribe()
            openSubscriptions[roomId] = sub
            roomConsumers[roomId] = consumer
        }

        return unsubscribeProxy(sub) {
            synchronized(openSubscriptions) {
                if (openSubscriptions[roomId] == sub) {
                    openSubscriptions.remove(roomId)
                }
            }
            roomConsumers.remove(roomId)
        }.connect()
    }

    private fun unsubscribeProxy(sub: ChatkitSubscription, hook: (Subscription) -> Unit) =
            object : ChatkitSubscription {
                override fun unsubscribe() {
                    hook(sub)
                    sub.unsubscribe()
                }

                override fun connect(): Subscription {
                    sub.connect()
                    return this
                }
            }

    fun close() {
        synchronized(openSubscriptions) {
            openSubscriptions.forEach { (_, sub) ->
                sub.unsubscribe()
            }
        }
    }

    private fun translateCursorEvent(event: ChatEvent): RoomEvent =
            when (event) {
                is ChatEvent.NewReadCursor -> RoomEvent.NewReadCursor(event.cursor)
                else -> RoomEvent.NoEvent
            }

    private fun enrichEvent(event: MembershipSubscriptionEvent): RoomEvent =
            when (event) {
                is MembershipSubscriptionEvent.UserJoined ->
                    userService.fetchUserBy(event.userId).map { user ->
                        RoomEvent.UserJoined(user) as RoomEvent
                    }.recover {
                        RoomEvent.ErrorOccurred(it)
                    }
                is MembershipSubscriptionEvent.UserLeft ->
                    userService.fetchUserBy(event.userId).map { user ->
                        RoomEvent.UserLeft(user) as RoomEvent
                    }.recover {
                        RoomEvent.ErrorOccurred(it)
                    }
                is MembershipSubscriptionEvent.ErrorOccurred -> {
                    RoomEvent.ErrorOccurred(event.error)
                }
                is MembershipSubscriptionEvent.InitialState -> {
                    logger.error("Should not have received membership initial state in RoomService")
                    RoomEvent.NoEvent
                }
            }

    private fun enrichEvent(event: RoomSubscriptionEvent, consumer: RoomConsumer): RoomEvent =
            when (event) {
                is RoomSubscriptionEvent.NewMessage ->
                    userService.fetchUserBy(event.message.userId).map { user ->
                        event.message.user = user
                        RoomEvent.Message(event.message) as RoomEvent
                    }.recover {
                        RoomEvent.ErrorOccurred(it)
                    }
                is RoomSubscriptionEvent.UserIsTyping ->
                    userService.fetchUserBy(event.userId).map { user ->
                        val onStop = {
                            consumer(RoomEvent.UserStoppedTyping(user))
                        }

                        if (scheduleStopTypingEvent(event.userId, onStop)) {
                            RoomEvent.UserStartedTyping(user)
                        } else {
                            RoomEvent.NoEvent
                        }
                    }.recover {
                        RoomEvent.ErrorOccurred(it)
                    }
                is RoomSubscriptionEvent.ErrorOccurred ->
                    RoomEvent.ErrorOccurred(event.error)
                is RoomSubscriptionEvent.NoEvent ->
                    RoomEvent.NoEvent
            }

    // Access synchronized on itself
    private val typingTimers = HashMap<String, Future<Unit>>()

    private fun scheduleStopTypingEvent(userId: String, onStop: () -> Unit): Boolean {
        synchronized(typingTimers) {
            val new = typingTimers[userId] == null
            if (!new) {
                typingTimers[userId]!!.cancel()
            }

            typingTimers[userId] = Futures.schedule {
                Thread.sleep(1_500)
                synchronized(typingTimers) {
                    typingTimers.remove(userId)
                }

                onStop()
            }

            return new
        }
    }

    fun distributeGlobalEvent(event: ChatEvent) {
        // This function must map events which we wish to report at room scope that
        // are not received at room scope from the backend.
        // Be careful, if you map an event where which originated here, you will create
        // an infinite loop consuming that event.
        when (event) {
            is ChatEvent.RoomUpdated ->
                roomConsumers[event.room.id]?.invoke(RoomEvent.RoomUpdated(event.room))
            is ChatEvent.RoomDeleted ->
                roomConsumers[event.roomId]?.invoke(RoomEvent.RoomDeleted(event.roomId))
            is ChatEvent.PresenceChange ->
                roomConsumers.keys.forEach { roomId ->
                    if (roomStore[roomId]?.memberUserIds?.contains(event.user.id) == true) {
                        roomConsumers[roomId]?.invoke(
                                RoomEvent.PresenceChange(event.user, event.currentState, event.prevState)
                        )
                    }
                }
        }
    }
}

private fun noRoomMembershipError(room: Room) : Error =
        Errors.other("User is not a member of ${room.name}")

internal data class UpdateRoomRequest(
        val name: String?,
        val private: Boolean?,
        val customData: CustomData?
)

private data class RoomCreateRequest(
    val name: String,
    val private: Boolean,
    val createdById: String,
    val customData: CustomData?,
    var userIds: List<String> = emptyList()
)
