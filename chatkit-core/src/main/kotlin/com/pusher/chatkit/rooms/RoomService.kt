package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatManagerEventConsumer
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.toJson
import com.pusher.platform.logger.Logger
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import elements.Subscription
import java.net.URLEncoder

internal class RoomService(
        private val client: PlatformClient,
        private val userService: UserService,
        private val cursorsService: CursorService,
        private val globalEventConsumers: MutableList<ChatManagerEventConsumer>,
        private val globalConsumer: (String) -> RoomConsumer,
        private val logger: Logger
) {
    private val openSubscriptions = HashMap<String, Subscription>()
    val roomStore = RoomStore()

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
            name: String,
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
            externalConsumer: RoomConsumer,
            messageLimit: Int
    ): Subscription {
        synchronized(openSubscriptions) {
            openSubscriptions[roomId]?.unsubscribe()
        }

        val sub = RoomSubscriptionGroup(
                messageLimit = messageLimit,
                roomId = roomId,
                userService = userService,
                cursorService = cursorsService,
                globalEventConsumers = globalEventConsumers,
                client = client,
                logger = logger,
                consumers = listOf(
                        applySideEffects(roomId),
                        globalConsumer(roomId),
                        externalConsumer
                )
        )
        synchronized(openSubscriptions) {
            openSubscriptions[roomId] = sub
        }

        return unsubscribeProxy(sub) {
            synchronized(openSubscriptions) {
                if (openSubscriptions[roomId] == sub) {
                    openSubscriptions.remove(roomId)
                }
            }
        }.connect()
    }

    private fun applySideEffects(roomId: String): RoomConsumer = { event ->
        when (event) {
            is RoomEvent.UserJoined ->
                roomStore[roomId]?.addUser(event.user.id)
            is RoomEvent.UserLeft ->
                roomStore[roomId]?.removeUser(event.user.id)
        }
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
}

private fun noRoomMembershipError(room: Room) : Error =
        Errors.other("User is not a member of ${room.name}")

internal data class UpdateRoomRequest(
        val name: String,
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
