package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.messages.multipart.*
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.makeSafe
import com.pusher.chatkit.util.toJson
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.util.*
import elements.Error
import elements.Errors
import elements.Subscription
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

internal class RoomService(
        private val v2client: PlatformClient,
        private val v3client: PlatformClient,
        private val urlRefresher: UrlRefresher,
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
                    .flatRecover { v3client.doGet("/rooms/${URLEncoder.encode(id, "UTF-8")}") }
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
            v3client.doGet<List<Room>>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms?joinable=$joinable")
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
                    .flatMap { body -> v3client.doPost<Room>("/rooms", body) }
                    .map { room ->
                        roomStore += room
                        userService.populateUserStore(room.memberUserIds)
                        room
                    }

    fun deleteRoom(roomId: String): Result<String, Error> =
            v3client.doDelete<Unit?>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}")
                    .map {
                        roomStore -= roomId
                        roomId
                    }

    fun leaveRoom(userId: String, roomId: String): Result<String, Error> =
            v3client.doPost<Unit?>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms/${URLEncoder.encode(roomId, "UTF-8")}/leave")
                    .map {
                        roomStore -= roomId
                        roomId
                    }

    fun joinRoom(userId: String, roomId: String): Result<Room, Error> =
            v3client.doPost<Room>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms/${URLEncoder.encode(roomId, "UTF-8")}/join")
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
                    .flatMap { body -> v3client.doPut<Unit>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}", body) }

    fun isSubscribedTo(roomId: String) =
            synchronized(openSubscriptions) {
                openSubscriptions[roomId] != null
            }

    fun subscribeToRoom(
            roomId: String,
            unsafeConsumer: RoomConsumer,
            messageLimit: Int
    ): Subscription =
            subscribeToRoom(roomId, unsafeConsumer, messageLimit, v2client, RoomSubscriptionEventParserV2)

    fun subscribeToRoomMultipart(
            roomId: String,
            unsafeConsumer: RoomConsumer,
            messageLimit: Int
    ): Subscription =
            subscribeToRoom(roomId, unsafeConsumer, messageLimit, v3client, RoomSubscriptionEventParserV3)

    private fun subscribeToRoom(
            roomId: String,
            unsafeConsumer: RoomConsumer,
            messageLimit: Int,
            client: PlatformClient,
            parser: DataParser<RoomSubscriptionEvent>
    ): Subscription {
        // This consumer is made safe by the layer providing it
        val globalConsumer = makeGlobalConsumer(roomId)
        val consumer: RoomConsumer = { event -> makeSafe(logger) { unsafeConsumer(event) } }

        val buffer = ArrayList<RoomEvent>()
        var ready = false
        val emit = { event: RoomEvent ->
            synchronized(buffer) {
                if (ready) {
                    consumer(event)
                    globalConsumer(event)
                } else {
                    buffer.add(event)
                }
            }
            Unit
        }

        val sub = RoomSubscriptionGroup(
                messageLimit = messageLimit,
                roomId = roomId,
                cursorService = cursorsService,
                membershipConsumer = { roomStore.applyMembershipEvent(roomId, it).map(::enrichEvent).forEach(emit) },
                roomConsumer = { emit(enrichEvent(it, emit)) },
                cursorConsumer = { emit(translateCursorEvent(it)) },
                client = client,
                messageParser = parser,
                logger = logger
        )
        synchronized(openSubscriptions) {
            openSubscriptions[roomId]?.unsubscribe()
            openSubscriptions[roomId] = sub
            roomConsumers[roomId] = consumer
        }

        val proxiedSub = unsubscribeProxy(sub) {
            synchronized(openSubscriptions) {
                if (openSubscriptions[roomId] == sub) {
                    openSubscriptions.remove(roomId)
                }
            }
            roomConsumers.remove(roomId)
        }.connect()

        synchronized(buffer) {
            buffer.forEach { event ->
                consumer(event)
                globalConsumer(event)
            }
            buffer.clear()
            ready = true
        }

        return proxiedSub
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
        roomStore.clear()
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
                is RoomSubscriptionEvent.NewMultipartMessage ->
                    userService.fetchUserBy(event.message.userId).flatMap { user ->
                        fetchRoomBy(event.message.userId, event.message.roomId).flatMap { room ->
                            event.message.parts.map(::makePart).collect().map { parts ->
                                RoomEvent.MultipartMessage(
                                        Message(
                                                id = event.message.id,
                                                parts = parts,
                                                room = room,
                                                sender = user,
                                                createdAt = event.message.createdAt,
                                                updatedAt = event.message.updatedAt
                                        )
                                ) as RoomEvent
                            }
                        }
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

    private fun makePart(body: V3PartBody): Result<Part, Error> =
            try {
                when {
                    body.content != null ->
                        Part(
                                partType = PartType.InlinePayload,
                                payload = Payload.Inline(
                                        type = body.type,
                                        content = body.content
                                )
                        ).asSuccess()
                    body.url != null ->
                        Part(
                                partType = PartType.UrlPayload,
                                payload = Payload.Url(
                                        type = body.type,
                                        url = URL(body.url)
                                )
                        ).asSuccess()
                    body.attachment != null ->
                        Part(
                                partType = PartType.AttachmentPayload,
                                payload = Payload.Attachment(
                                        type = body.type,
                                        size = body.attachment.size,
                                        name = body.attachment.name,
                                        customData = body.attachment.customData,
                                        refreshUrl = body.attachment.refreshUrl,
                                        downloadUrl = body.attachment.downloadUrl,
                                        expiration = body.attachment.expiration,
                                        refresher = urlRefresher
                                )
                        ).asSuccess()
                    else ->
                        Errors.other("Invalid part entity, no content, url or attachment found.").asFailure()
                }
            } catch (e: Exception) {
                Errors.other(e).asFailure()
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

private fun noRoomMembershipError(room: Room): Error =
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
