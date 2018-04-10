package com.pusher.chatkit.rooms

import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.chatkit.network.toJson
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.*
import elements.Error
import elements.OtherError
import okhttp3.HttpUrl

typealias RoomListResult = Result<List<Room>, Error>
typealias RoomResult = Result<Room, Error>
typealias RoomListResultPromise = Promise<RoomListResult>
typealias RoomPromiseResult = Promise<RoomResult>

class RoomService(
    // TODO: remove dependency to current user
    private val currentUser: CurrentUser,
    private val chatManager: ChatManager
) {

    fun fetchRoomBy(id: Int): RoomPromiseResult =
        getLocalRoom(id).recover {
            chatManager.doGet("/rooms/$id")
                .parseResponseWhenReady()
        }.flatMapResult { room ->
            when {
                room.memberUserIds.contains(currentUser.id) -> room.asSuccess()
                else -> NoRoomMembershipError(room).asFailure<Room, Error>()
            }.asPromise()
        }

    private fun getLocalRoom(id: Int) =
        chatManager.roomStore[id]?.asSuccess<Room, Error>()?.asPromise()
            .orElse { OtherError("User not found locally") }

    @JvmOverloads
    fun fetchUserRooms(onlyJoinable: Boolean = false): RoomListResultPromise =
        chatManager.doGet("/users/${currentUser.id}/rooms?joinable=$onlyJoinable")
            .parseResponseWhenReady<List<Room>>().onReady { result ->
                chatManager.roomStore += result.recover { emptyList() }
            }


    fun joinRoom(room: Room): RoomPromiseResult =
        joinRoom(room.id)

    fun joinRoom(roomId: Int): RoomPromiseResult =
        chatManager.doPost(roomIdJoinPath(roomId))
            .parseResponseWhenReady<Room>()
            .updateStoreWhenReady()

    private fun roomIdJoinPath(roomId: Int) =
        HttpUrl.parse("https://pusherplatform.io")!!.newBuilder().addPathSegments("/users/${currentUser.id}/rooms/$roomId/join").build().encodedPath()


    @JvmOverloads
    fun createRoom(
        name: String,
        isPrivate: Boolean = false,
        userIds: List<String> = emptyList()
    ): RoomPromiseResult =
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

    fun roomFor(roomAware: HasRoom) =
        fetchRoomBy(roomAware.roomId)

    private fun RoomPromiseResult.updateStoreWhenReady() = onReady {
        it.map { room ->
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

data class NoRoomMembershipError(val room: Room) : Error {
    override val reason: String = "User is not a member of ${room.name}"
}
