package com.pusher.chatkit.rooms

import com.pusher.chatkit.*
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.toFuture
import com.pusher.util.*
import elements.Error
import elements.Errors
import java.util.concurrent.Future

internal class RoomService(private val chatManager: ChatManager) {

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

    @JvmOverloads
    fun fetchUserRooms(userId: String, onlyJoinable: Boolean = false): Future<Result<List<Room>, Error>> =
        chatManager.doGet<List<Room>>("/users/$userId/rooms?joinable=$onlyJoinable")
            .mapResult { rooms -> rooms.also { chatManager.roomStore += it } }

    fun joinRoom(userId: String, room: Room): Future<Result<Room, Error>> =
        joinRoom(userId, room.id)

    fun joinRoom(userId: String, roomId: Int): Future<Result<Room, Error>> =
        chatManager.doPost<Room>("/users/$userId/rooms/$roomId/join")
            .updateStoreWhenReady()

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

    private fun Future<Result<Room, Error>>.updateStoreWhenReady() = mapResult {
        it.also { room ->
            chatManager.roomStore += room
            populateRoomUserStore(room)
        }
    }

    private fun populateRoomUserStore(room: Room) {
        chatManager.userService().populateUserStore(room.memberUserIds)
    }

}

interface HasRoom {
    val roomId: Int
}

private fun noRoomMembershipError(room: Room) : Error =
    Errors.other("User is not a member of ${room.name}")
