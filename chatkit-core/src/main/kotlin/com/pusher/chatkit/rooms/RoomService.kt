package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.HasChat
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.cursors.CursorService
import com.pusher.chatkit.users.UserService
import com.pusher.chatkit.util.toJson
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.toFuture
import com.pusher.util.*
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal class RoomService(
        override val chatManager: ChatManager,
        private val client: PlatformClient,
        private val userService: UserService,
        private val cursorsService: CursorService,
        private val globalConsumer: (Int) -> RoomConsumer,
        private val logger: Logger
) : HasChat {

    val roomStore by lazy { RoomStore() }

    fun fetchRoomBy(userId: String, id: Int): Future<Result<Room, Error>> =
            getLocalRoom(id).toFuture()
                    .recoverFutureResult { client.doGet("/rooms/$id") }
                    .flatMapResult { room ->
                        when {
                            room.memberUserIds.contains(userId) -> room.asSuccess()
                            else -> noRoomMembershipError(room).asFailure<Room, Error>()
                        }
                    }

    private fun getLocalRoom(id: Int): Result<Room, Error> =
            roomStore[id]
                    .orElse { Errors.other("User not found locally") }

    fun fetchUserRooms(userId: String, joinable: Boolean = false): Future<Result<List<Room>, Error>> =
            client.doGet<List<Room>>("/users/$userId/rooms?joinable=$joinable")
                    .mapResult { rooms -> rooms.also { roomStore += it } }

    fun createRoom(
            creatorId: String,
            name: String,
            isPrivate: Boolean,
            userIds: List<String>
    ): Future<Result<Room, Error>> =
            RoomCreateRequest(
                    name = name,
                    private = isPrivate,
                    createdById = creatorId,
                    userIds = userIds
            ).toJson()
                    .toFuture()
                    .flatMapFutureResult { body -> client.doPost<Room>("/rooms", body) }
                    .saveRoomWhenReady()

    fun roomFor(userId: String, roomAware: HasRoom) =
            fetchRoomBy(userId, roomAware.roomId)

    fun deleteRoom(roomId: Int): Future<Result<Int, Error>> =
            client.doDelete<Unit?>("/rooms/$roomId")
                    .mapResult { roomId }
                    .removeRoomWhenReady()


    fun leaveRoom(userId: String, roomId: Int): Future<Result<Int, Error>> =
            client.doPost<Unit?>("/users/$userId/rooms/$roomId/leave")
                    .mapResult { roomId }
                    .removeRoomWhenReady()

    fun joinRoom(userId: String, roomId: Int): Future<Result<Room, Error>> =
            client.doPost<Room>("/users/$userId/rooms/$roomId/join")
                    .saveRoomWhenReady()

    fun updateRoom(roomId: Int, name: String, isPrivate: Boolean? = null): Future<Result<Unit, Error>> =
            UpdateRoomRequest(name, isPrivate).toJson().toFuture()
                    .flatMapFutureResult { body -> client.doPut<Unit>("/rooms/$roomId", body) }

    fun subscribeToRoom(
            roomId: Int,
            externalConsumer: RoomConsumer,
            messageLimit: Int
    ): Subscription =
            RoomSubscriptionGroup(
                    messageLimit,
                    roomId,
                    userService,
                    cursorsService,
                    client,
                    logger,
                    listOf(applySideEffects(roomId), globalConsumer(roomId), externalConsumer)
            ).connect()

    private fun applySideEffects(roomId: Int): RoomConsumer = { event ->
        when (event) {
            is RoomEvent.UserJoined ->
                roomStore[roomId]?.addUser(event.user.id)
            is RoomEvent.UserLeft ->
                roomStore[roomId]?.removeUser(event.user.id)
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
