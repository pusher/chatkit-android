package com.pusher.chatkit

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.ChatManager.Companion.GSON
import com.pusher.chatkit.users.UserListResultPromise
import com.pusher.platform.Instance
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Promise
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.mapResult
import elements.Error
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import okhttp3.HttpUrl

typealias ConfirmationPromise = Promise<Result<Boolean, Error>>

class CurrentUser(
    val apiInstance: Instance,
    val createdAt: String,
    val cursors: MutableMap<Int, Cursor>,
    val cursorsInstance: Instance,
    val id: String,
    val logger: Logger,
    val filesInstance: Instance,
    val presenceInstance: Instance,
    val tokenParams: ChatkitTokenParams?,
    val tokenProvider: TokenProvider,
    val userStore: GlobalUserStore,
    var avatarURL: String?,
    var customData: CustomData?,
    var name: String?,
    var updatedAt: String,
    private val chatManager: ChatManager
) {

    val rooms: List<Room> get() = chatManager.roomStore.toList()
        .filter { it.memberUserIds.contains(id) }
    val users: UserListResultPromise get() = rooms
        .flatMap { it.memberUserIds }
        .toSet()
        .let { ids -> chatManager.userService().fetchUsersBy(ids) }

    fun updateWithPropertiesOf(newUser: User) {
        updatedAt = newUser.updatedAt
        name = newUser.name
        customData = newUser.customData
    }

    val presenceSubscription: PresenceSubscription by lazy {
        PresenceSubscription(
            instance = presenceInstance,
            path = "/users/$id/presence",
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            chatManager = chatManager
        )
    }

    fun setCursor(
        position: Int,
        room: Room
    ) = cursorsInstance.request(
        options = RequestOptions(
            method = "PUT",
            path = "/cursors/0/rooms/${room.id}/users/$id",
            body = GSON.toJson(SetCursorRequest(position))
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    )

    fun fetchAttachment(attachmentUrl: String) = filesInstance.request(
        options = RequestOptions(
            method = "GET",
            destination = RequestDestination.Absolute(attachmentUrl)
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams

    ).mapResult {
        GSON.fromJson<FetchedAttachment>(it.body()!!.charStream(), FetchedAttachment::class.java)
    }

    fun addUsers(roomId: Int, users: Array<User>) = addUsers(roomId, users.map { id }.toTypedArray())
    fun addUsers(roomId: Int, userIds: Array<String>) = addOrRemoveUsers("add", roomId, userIds)

    fun removeUsers(roomId: Int, users: Array<User>) = removeUsers(roomId, users.map { id }.toTypedArray())
    fun removeUsers(roomId: Int, userIds: Array<String>) = addOrRemoveUsers("remove", roomId, userIds)

    private fun addOrRemoveUsers(
        operation: String,
        roomId: Int,
        userIds: Array<String>
    ): ConfirmationPromise {

        val data = object {
            val userIds = userIds
        }

        return apiInstance.request(
            options = RequestOptions(
                method = "PUT",
                path = "/rooms/$roomId/users/$operation",
                body = GSON.toJson(data)
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams
        ).mapResult { it.isSuccessful }
    }

    /**
     * Update a room
     * */

    fun updateRoom(
        room: Room,
        name: String? = null,
        isPrivate: Boolean? = null
    ): ConfirmationPromise {
        val path = "/rooms/${room.id}"
        val data = UpdateRoomRequest(
            name = name ?: room.name,
            isPrivate = isPrivate ?: room.isPrivate
        )

        return apiInstance.request(
            options = RequestOptions(
                method = "PUT",
                path = path,
                body = GSON.toJson(data)
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams
        ).mapResult { it.isSuccessful }
    }

    data class UpdateRoomRequest(val name: String, val isPrivate: Boolean)

    /**
     * Delete a room
     * */
    fun deleteRoom(room: Room): ConfirmationPromise =
        deleteRoom(room.id)

    fun deleteRoom(roomId: Int): ConfirmationPromise = apiInstance.request(
        options = RequestOptions(
            method = "DELETE",
            path = "/rooms/$roomId",
            body = ""
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).mapResult { it.isSuccessful }

    /**
     * Leave a room
     * */
    fun leaveRoom(room: Room): ConfirmationPromise =
        leaveRoom(room.id)

    fun leaveRoom(roomId: Int): ConfirmationPromise = apiInstance.request(
        options = RequestOptions(
            method = "POST",
            path = leaveRoomPath(roomId),
            body = "" //TODO: this is a horrible OKHTTP hack - POST is required to have a body.
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).mapResult { it.isSuccessful }

    // TODO(pga): investigate why is this scaped this way
    private fun leaveRoomPath(roomId: Int) =
        HttpUrl.parse("https://pusherplatform.io")!!.newBuilder().addPathSegments("/users/$id/rooms/$roomId/leave").build().encodedPath()

    val presenceEvents: SubscriptionReceiveChannel<ChatManagerEvent>
        get() = presenceSubscription.openSubscription()

}

data class MessageRequest(val text: String? = null, val userId: String, val attachment: AttachmentBody? = null)

sealed class AttachmentBody {
    data class Resource(val resourceLink: String, val type: String) : AttachmentBody()
    object None : AttachmentBody()
    data class Failed(val error: Error) : AttachmentBody()
}

data class SetCursorRequest(val position: Int)

data class RoomCreateRequest(
    val name: String,
    val private: Boolean,
    val createdById: String,
    var userIds: List<String> = emptyList()
)

data class FetchedAttachment(
    val file: FetchedAttachmentFile,
    @SerializedName("resource_link") val link: String,
    val ttl: Double
)

data class FetchedAttachmentFile(val bytes: Int, val lastModified: Double, val name: String)
