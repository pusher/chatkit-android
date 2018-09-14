package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatManagerEventConsumer
import com.pusher.chatkit.HasChat
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

internal class RoomService(
        // TODO still here (see HasChat)
        override val chatManager: ChatManager,
        private val client: PlatformClient,
        private val userService: UserService,
        private val cursorsService: CursorService,
        private val globalEventConsumers: MutableList<ChatManagerEventConsumer>,
        private val globalConsumer: (Int) -> RoomConsumer,
        private val logger: Logger
) : HasChat {
    private val openSubscriptions = HashMap<Int, Subscription>()
    val roomStore = RoomStore()

    fun fetchRoomBy(userId: String, id: Int): Result<Room, Error> =
            getLocalRoom(id)
                    .flatRecover { client.doGet("/rooms/$id") }
                    .flatMap { room ->
                        when {
                            room.memberUserIds.contains(userId) -> room.asSuccess()
                            else -> noRoomMembershipError(room).asFailure<Room, Error>()
                        }
                    }

    private fun getLocalRoom(id: Int): Result<Room, Error> =
            roomStore[id]
                    .orElse { Errors.other("User not found locally") }

    fun fetchUserRooms(userId: String, joinable: Boolean = false): Result<List<Room>, Error> =
            client.doGet<List<Room>>("/users/$userId/rooms?joinable=$joinable")
                    .map { rooms -> rooms.also { roomStore += it } }

    fun createRoom(
            creatorId: String,
            name: String,
            isPrivate: Boolean,
            userIds: List<String>
    ): Result<Room, Error> =
            RoomCreateRequest(
                    name = name,
                    private = isPrivate,
                    createdById = creatorId,
                    userIds = userIds
            ).toJson()
                    .flatMap { body -> client.doPost<Room>("/rooms", body) }
                    .saveRoomWhenReady()

    fun roomFor(userId: String, roomAware: HasRoom) =
            fetchRoomBy(userId, roomAware.roomId)

    fun deleteRoom(roomId: Int): Result<Int, Error> =
            client.doDelete<Unit?>("/rooms/$roomId")
                    .map { roomId }
                    .removeRoomWhenReady()


    fun leaveRoom(userId: String, roomId: Int): Result<Int, Error> =
            client.doPost<Unit?>("/users/$userId/rooms/$roomId/leave")
                    .map { roomId }
                    .removeRoomWhenReady()

    fun joinRoom(userId: String, roomId: Int): Result<Room, Error> =
            client.doPost<Room>("/users/$userId/rooms/$roomId/join")
                    .saveRoomWhenReady()

    fun updateRoom(roomId: Int, name: String, isPrivate: Boolean? = null): Result<Unit, Error> =
            UpdateRoomRequest(name, isPrivate).toJson()
                    .flatMap { body -> client.doPut<Unit>("/rooms/$roomId", body) }

    fun isSubscribedTo(roomId: Int) =
            synchronized(openSubscriptions) {
                openSubscriptions[roomId] != null
            }

    fun subscribeToRoom(
            roomId: Int,
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

    private fun applySideEffects(roomId: Int): RoomConsumer = { event ->
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

internal data class UpdateRoomRequest(val name: String, val isPrivate: Boolean?)

/**
 * Used by [RoomService.roomFor] so an object can say that they have a room
 */
interface HasRoom {
    val roomId: Int
}

private fun noRoomMembershipError(room: Room) : Error =
    Errors.other("User is not a member of ${room.name}")

private data class RoomCreateRequest(
    val name: String,
    val private: Boolean,
    val createdById: String,
    var userIds: List<String> = emptyList()
)
