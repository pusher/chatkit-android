package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.messages.multipart.UrlRefresher
import com.pusher.chatkit.messages.multipart.upgradeMessageV3
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
  data class Override(val title: String): RoomPushNotificationTitle()
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

    internal val roomStore = RoomStore()

    internal fun populateInitial(rooms: List<Room>) {
        roomStore.initialiseContents(rooms)
    }

    fun fetchRoom(id: String): Result<Room, Error> =
            getLocalRoom(id).flatRecover {
                client.doGet("/rooms/${URLEncoder.encode(id, "UTF-8")}")
            }

    private fun getLocalRoom(id: String): Result<Room, Error> =
            roomStore[id]
                    .orElse { Errors.other("Room not found locally") }

    fun fetchUserRooms(userId: String, joinable: Boolean = false): Result<List<Room>, Error> =
            client.doGet<List<Room>>("/users/${URLEncoder.encode(userId, "UTF-8")}/rooms?joinable=$joinable")
                    .map { rooms -> rooms.also { roomStore += it } }

    fun createRoom(
            id: String?,
            creatorId: String,
            name: String,
            pushNotificationTitleOverride: String?,
            isPrivate: Boolean,
            customData: CustomData?,
            userIds: List<String>
    ): Result<Room, Error> =
            RoomCreateRequest(
                    id = id,
                    name = name,
                    pushNotificationTitleOverride = pushNotificationTitleOverride,
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
            legacyV2client.doDelete<Unit?>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}")
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
              UpdateRoomRequestWithPNTitleOverride(name, null, isPrivate, customData)
            is RoomPushNotificationTitle.Override ->
              UpdateRoomRequestWithPNTitleOverride(name, pushNotificationTitleOverride.title, isPrivate, customData)
          }
        }

      return request
          .toJson()
          .flatMap { body -> client.doPut<Unit>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}", body) }
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

    // TODO: call (from SynchronousChatManager?), split responsibility (enrich/translation)?,
    //  find new home? (SynchronousChatManager or maybe separate SRP classes)
    // TODO: when this is called (TODO above) the events shall be emitted
    private fun enrichEvents(events: List<UserSubscriptionEvent>): List<RoomEvent> {
        val allUserIds = events.map {
            when (it) {
                is UserSubscriptionEvent.UserJoinedRoomEvent -> it.userId
                is UserSubscriptionEvent.UserLeftRoomEvent -> it.userId
                else -> null
            }
        }.filterNotNull().toSet()

        return userService.fetchUsersBy(allUserIds).map { users ->
            events.map { event ->
                when (event) {
                    is UserSubscriptionEvent.UserJoinedRoomEvent -> {
                        val user = users[event.userId]
                        when (user) {
                            null -> RoomEvent.ErrorOccurred(Errors.other("Could not find user with id ${event.userId}"))
                            else -> RoomEvent.UserJoined(user)
                        }
                    }
                    is UserSubscriptionEvent.UserLeftRoomEvent -> {
                        val user = users[event.userId]
                        when (user) {
                            null -> RoomEvent.ErrorOccurred(Errors.other("Could not find user with id ${event.userId}"))
                            else -> RoomEvent.UserLeft(user)
                        }
                    }
                    is UserSubscriptionEvent.ErrorOccurred -> {
                        RoomEvent.ErrorOccurred(event.error)
                    }
                    is UserSubscriptionEvent.InitialState -> {
                        logger.error("Should not have received membership initial state in RoomService")
                        RoomEvent.NoEvent
                    }
                    else -> RoomEvent.NoEvent
                }
            }
        }.recover {
            listOf(RoomEvent.ErrorOccurred(it))
        }
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

private fun noRoomMembershipError(room: Room): Error =
        Errors.other("User is not a member of ${room.name}")

internal data class UpdateRoomRequest(
        val name: String?,
        val private: Boolean?,
        val customData: CustomData?
)

internal data class UpdateRoomRequestWithPNTitleOverride(
        val name: String?,
        val pushNotificationTitleOverride: String?,
        val private: Boolean?,
        val customData: CustomData?
)

private data class RoomCreateRequest(
        val id: String?,
        val name: String,
        val pushNotificationTitleOverride: String?,
        val private: Boolean,
        val createdById: String,
        val customData: CustomData?,
        var userIds: List<String> = emptyList()
)
