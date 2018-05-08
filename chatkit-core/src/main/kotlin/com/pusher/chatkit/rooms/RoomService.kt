package com.pusher.chatkit.rooms

import com.pusher.chatkit.*
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.toFuture
import com.pusher.util.*
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.Future

internal fun ChatManager.roomService(): RoomService =
    RoomService(this)

internal class RoomService(override val chatManager: ChatManager) : HasChat {

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
        chatManager.roomStore[id]
            .orElse { Errors.other("User not found locally") }

    fun fetchUserRooms(userId: String, onlyJoinable: Boolean = false): Future<Result<List<Room>, Error>> =
        chatManager.doGet<List<Room>>("/users/$userId/rooms?joinable=$onlyJoinable")
            .mapResult { rooms -> rooms.also { chatManager.roomStore += it } }

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
            .updateStoreWhenReady()

    fun roomFor(userId: String, roomAware: HasRoom) =
        fetchRoomBy(userId, roomAware.roomId)

    fun deleteRoom(roomId: Int): Future<Result<String, Error>> =
        chatManager.doDelete("/rooms/$roomId")

    fun leaveRoom(userId: String, roomId: Int): Future<Result<Unit, Error>> =
        chatManager.doPost("/users/$userId/rooms/$roomId/leave")

    fun joinRoom(userId: String, roomId: Int): Future<Result<Room, Error>> =
        chatManager.doPost("/users/$userId/rooms/$roomId/join")

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
