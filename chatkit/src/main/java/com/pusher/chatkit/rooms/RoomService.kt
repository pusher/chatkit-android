package com.pusher.chatkit.rooms

import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.mapResult
import com.pusher.util.orElse
import elements.Error
import elements.OtherError
import okhttp3.HttpUrl

typealias RoomListResult = Result<List<Room>, Error>
typealias RoomResult = Result<Room, Error>
typealias RoomListFutureResult = Promise<Result<List<Room>, Error>>
typealias RoomFutureResult = Promise<Result<Room, Error>>

class RoomService(
    private val currentUser: CurrentUser,
    private val chatManager: ChatManager
) {

    fun findBy(id: Int): RoomResult =
        currentUser.rooms()
            .firstOrNull { it.id == id }
            .orElse { OtherError("Room $id not found") as Error }

    fun findAll() =
        currentUser.rooms()

    fun fetchRoomBy(id: Int): RoomFutureResult =
        chatManager.doGet("/rooms/$id")
            .parseResponseWhenReady()

    @JvmOverloads
    fun fetchUserRooms(onlyJoinable: Boolean = false): RoomListFutureResult =
        chatManager.doGet("/users/${currentUser.id}/rooms?joinable=$onlyJoinable")
            .parseResponseWhenReady()

    fun joinRoom(room: Room): RoomFutureResult =
        joinRoom(room.id)

    fun joinRoom(roomId: Int): RoomFutureResult =
        chatManager.doGet(roomIdJoinPath(roomId))
            .parseResponseWhenReady<Room>()
            .updateStoreWhenReady()

    private fun roomIdJoinPath(roomId: Int) =
        HttpUrl.parse("https://pusherplatform.io")!!.newBuilder().addPathSegments("/users/${currentUser.id}/rooms/$roomId/join").build().encodedPath()


    //Room membership related information
    @JvmOverloads
    fun createRoom(
        name: String,
        isPrivate: Boolean = false,
        userIds: List<String> = emptyList()
    ): RoomFutureResult =
        RoomCreateRequest(
            name = name,
            private = isPrivate,
            createdById = currentUser.id,
            userIds = userIds
        ).toJson()
            .map { body -> chatManager.doPost("/rooms", body) }
            .fold(
                { error -> error.asFailure<Room, Error>().asPromise() },
                { promise -> promise.parseResponseWhenReady() }
            )
            .updateStoreWhenReady()


    private fun RoomFutureResult.updateStoreWhenReady() = this.also {
        onReady {
            it.map { room ->
                currentUser.roomStore.addOrMerge(room)
                populateRoomUserStore(room)
            }
        }
    }

    private fun populateRoomUserStore(room: Room): List<Promise<Result<User, Error>>> {
        return room.memberUserIds.map { userId ->
            chatManager.userStore.findOrGetUser(userId).mapResult {
                it.also { user ->
                    room.userStore.addOrMerge(user)
                }
            }
        }
    }

}
