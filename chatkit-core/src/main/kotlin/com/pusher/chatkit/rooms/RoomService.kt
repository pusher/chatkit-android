package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.messages.multipart.UrlRefresher
import com.pusher.chatkit.messages.multipart.upgradeMessageV3
import com.pusher.chatkit.rooms.api.CreateRoomRequest
import com.pusher.chatkit.rooms.api.CreateRoomResponse
import com.pusher.chatkit.rooms.api.JoinRoomResponse
import com.pusher.chatkit.rooms.api.JoinableRoomsResponse
import com.pusher.chatkit.rooms.api.JoinedRoomApiMapper
import com.pusher.chatkit.rooms.api.NotJoinedRoomApiMapper
import com.pusher.chatkit.rooms.api.UpdateRoomRequest
import com.pusher.chatkit.rooms.api.UpdateRoomRequestWithPushNotificationTitleOverride
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.util.makeSafe
import com.pusher.chatkit.util.toJson
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.util.Result
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import elements.Subscription
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

sealed class RoomPushNotificationTitle {
    data class Override(val title: String) : RoomPushNotificationTitle()
    object NoOverride : RoomPushNotificationTitle()
}

internal class RoomService(
    private val legacyV2client: PlatformClient,
    private val client: PlatformClient,
    private val urlRefresher: UrlRefresher,
    private val userService: UserService,
    private val cursorsService: CursorService,
    private val makeGlobalConsumer: (String) -> RoomConsumer,
    private val logger: Logger
) {
    // Access synchronised on itself
    private val openSubscriptions = HashMap<String, Subscription>()
    private val roomConsumers = ConcurrentHashMap<String, RoomConsumer>()

    val roomStore = RoomStore()

    private val joinedRoomApiMapper = JoinedRoomApiMapper()
    private val notJoinedRoomApiMapper = NotJoinedRoomApiMapper()

    fun populateInitial(event: UserSubscriptionEvent.InitialState) {
        roomStore.initialiseContents(event.rooms, event.memberships, event.readStates)
    }

    fun getJoinedRoom(id: String): Result<Room, Error> =
            roomStore[id].orElse { Errors.other("Room not found locally") }

    fun fetchJoinableRooms(userId: String): Result<List<Room>, Error> =
            client.doGet<JoinableRoomsResponse>(
                    "/users/${URLEncoder.encode(userId, "UTF-8")}/joinable_rooms"
            ).map(notJoinedRoomApiMapper::toRooms)

    fun createRoom(
        id: String?,
        creatorId: String,
        name: String,
        pushNotificationTitleOverride: String?,
        isPrivate: Boolean,
        customData: CustomData?,
        userIds: List<String>
    ): Result<Room, Error> =
            CreateRoomRequest(
                    id = id,
                    name = name,
                    pushNotificationTitleOverride = pushNotificationTitleOverride,
                    private = isPrivate,
                    createdById = creatorId,
                    customData = customData,
                    userIds = userIds
            ).toJson()
                    .flatMap { body -> client.doPost<CreateRoomResponse>("/rooms", body) }
                    .map { response ->
                        roomStore.add(response)
                        userService.populateUserStore(response.membership.userIds.toSet())
                        joinedRoomApiMapper.toRoom(response)
                    }

    fun deleteRoom(roomId: String): Result<String, Error> =
            legacyV2client.doDelete<Unit?>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}")
                    .map {
                        roomStore.remove(roomId)
                        roomId
                    }

    fun leaveRoom(userId: String, roomId: String): Result<String, Error> =
            client.doPost<Unit?>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms/${URLEncoder.encode(roomId, "UTF-8")}/leave")
                    .map {
                        roomStore.remove(roomId)
                        roomId
                    }

    fun joinRoom(userId: String, roomId: String): Result<Room, Error> =
            client.doPost<JoinRoomResponse>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms/${URLEncoder.encode(roomId, "UTF-8")}/join")
                    .map { response ->
                        roomStore.add(response)
                        userService.populateUserStore(response.membership.userIds.toSet())
                        joinedRoomApiMapper.toRoom(response)
                    }

    fun updateRoom(
        roomId: String,
        name: String? = null,
        pushNotificationTitleOverride: RoomPushNotificationTitle? = null,
        isPrivate: Boolean? = null,
        customData: CustomData? = null
    ): Result<Unit, Error> {
        val request: Any =
                if (pushNotificationTitleOverride == null) {
                    UpdateRoomRequest(name, isPrivate, customData)
                } else {
                    when (pushNotificationTitleOverride) {
                        is RoomPushNotificationTitle.NoOverride ->
                            UpdateRoomRequestWithPushNotificationTitleOverride(name, null, isPrivate, customData)
                        is RoomPushNotificationTitle.Override ->
                            UpdateRoomRequestWithPushNotificationTitleOverride(name,
                                    pushNotificationTitleOverride.title, isPrivate, customData)
                    }
                }

        return request
                .toJson()
                .flatMap { body ->
                    client.doPut<Unit>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}", body)
                }
    }

    fun isSubscribedTo(roomId: String) =
            synchronized(openSubscriptions) {
                openSubscriptions[roomId] != null
            }

    fun subscribeToRoom(
        roomId: String,
        unsafeConsumer: RoomConsumer,
        messageLimit: Int
    ): Subscription =
            subscribeToRoom(roomId, unsafeConsumer, messageLimit, legacyV2client, RoomSubscriptionEventParserV2)

    fun subscribeToRoomMultipart(
        roomId: String,
        unsafeConsumer: RoomConsumer,
        messageLimit: Int
    ): Subscription =
            subscribeToRoom(roomId, unsafeConsumer, messageLimit, client, RoomSubscriptionEventParserV3)

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

        // ensure members are fetched and presence subscription is opened for each of them
        val usersFetchResult = userService.fetchUsersBy(roomStore[roomId]!!.memberUserIds)
        if (usersFetchResult is Result.Failure) {
            emit(RoomEvent.ErrorOccurred(usersFetchResult.error))
        }

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

    private fun enrichEvent(event: RoomSubscriptionEvent, consumer: RoomConsumer): RoomEvent =
            when (event) {
                is RoomSubscriptionEvent.NewMessage ->
                    userService.fetchUserBy(event.message.userId).map { user ->
                        event.message.user = user
                        @Suppress("DEPRECATION") // we're translating here from the legacy event
                        RoomEvent.Message(event.message) as RoomEvent
                    }.recover {
                        RoomEvent.ErrorOccurred(it)
                    }
                is RoomSubscriptionEvent.NewMultipartMessage ->
                    upgradeMessageV3(
                            event.message,
                            this,
                            userService,
                            urlRefresher
                    ).map {
                        RoomEvent.MultipartMessage(it) as RoomEvent
                    }.recover {
                        RoomEvent.ErrorOccurred(it)
                    }
                is RoomSubscriptionEvent.MessageDeleted ->
                    RoomEvent.MessageDeleted(event.messageId)
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
        // Be careful, if you map an event which originated here, you will create
        // an infinite loop consuming that event.
        when (event) {
            is ChatEvent.RoomUpdated ->
                roomConsumers[event.room.id]?.invoke(RoomEvent.RoomUpdated(event.room))
            is ChatEvent.RoomDeleted ->
                roomConsumers[event.roomId]?.invoke(RoomEvent.RoomDeleted(event.roomId))
            is ChatEvent.UserJoinedRoom ->
                roomConsumers[event.room.id]?.invoke(RoomEvent.UserJoined(event.user))
            is ChatEvent.UserLeftRoom ->
                roomConsumers[event.room.id]?.invoke(RoomEvent.UserLeft(event.user))
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
