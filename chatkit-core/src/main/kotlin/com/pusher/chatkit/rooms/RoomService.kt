package com.pusher.chatkit.rooms

import com.pusher.chatkit.*
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.map
import com.pusher.platform.network.toFuture
import com.pusher.util.*
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal class RoomService(override val chatManager: ChatManager) : HasChat {

    val roomStore by lazy { RoomStore() }

    fun fetchRoomBy(userId: String, id: Int): Future<Result<Room, Error>> =
        getLocalRoom(id).toFuture()
            .recoverFutureResult { chatManager.doGet("/rooms/$id") }
            .flatMapResult { room ->
                when {
                    room.memberUserIds.contains(userId) -> room.asSuccess()
                    else -> noRoomMembershipError(room).asFailure<Room, Error>()
                }
            }

    private fun getLocalRoom(id: Int): Result<Room, Error> =
        roomStore[id]
            .orElse { Errors.other("User not found locally") }

    fun fetchUserRooms(userId: String, onlyJoinable: Boolean = false): Future<Result<List<Room>, Error>> =
        chatManager.doGet<List<Room>>("/users/$userId/rooms?joinable=$onlyJoinable")
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
            .flatMapFutureResult { body -> chatManager.doPost<Room>("/rooms", body) }
            .saveRoomWhenReady()

    fun roomFor(userId: String, roomAware: HasRoom) =
        fetchRoomBy(userId, roomAware.roomId)

    fun deleteRoom(roomId: Int): Future<Result<Int, Error>> =
        chatManager.doDelete<Unit?>("/rooms/$roomId")
            .mapResult { roomId }
            .removeRoomWhenReady()


    fun leaveRoom(userId: String, roomId: Int): Future<Result<Int, Error>> =
        chatManager.doPost<Unit?>("/users/$userId/rooms/$roomId/leave")
            .mapResult { roomId }
            .removeRoomWhenReady()

    fun joinRoom(userId: String, roomId: Int): Future<Result<Room, Error>> =
        chatManager.doPost<Room>("/users/$userId/rooms/$roomId/join")
            .saveRoomWhenReady()

    fun updateRoom(roomId: Int, name: String, isPrivate: Boolean? = null): Future<Result<Unit, Error>> =
        UpdateRoomRequest(name,isPrivate).toJson().toFuture()
            .flatMapFutureResult { body -> chatManager.doPut<Unit>("/rooms/$roomId", body) }

    fun subscribeToRoom(
        userId: String,
        roomId: Int,
        listeners: RoomSubscriptionConsumer,
        messageLimit : Int
    ): Subscription =
        RoomSubscription(roomId, userId, listeners, chatManager, messageLimit)

}

internal data class UpdateRoomRequest(val name: String, val isPrivate: Boolean?)

interface HasRoom {
    val roomId: Int
}

private fun noRoomMembershipError(room: Room) : Error =
    Errors.other("User is not a member of ${room.name}")
