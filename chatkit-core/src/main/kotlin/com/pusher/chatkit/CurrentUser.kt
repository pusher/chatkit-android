package com.pusher.chatkit

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.ChatManager.Companion.GSON
import com.pusher.platform.Instance
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.mapResult
import elements.Error
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import java.util.concurrent.Future

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
    val users: Future<Result<List<User>, Error>>
        get() = rooms
        .flatMap { it.memberUserIds }
        .let { ids -> chatManager.userService().fetchUsersBy(ids.toSet()) }

    val roomSubscriptions: List<RoomSubscription>
        get() = _roomSubscriptions

    private val _roomSubscriptions = mutableListOf<RoomSubscription>()

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
    ): Future<Result<Boolean, Error>> = cursorsInstance.request<String>(
        options = RequestOptions(
            method = "PUT",
            path = "/cursors/0/rooms/${room.id}/users/$id",
            body = GSON.toJson(SetCursorRequest(position))
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).mapResult { true }

    fun fetchAttachment(attachmentUrl: String) = filesInstance.request<FetchedAttachment>(
        options = RequestOptions(
            method = "GET",
            destination = RequestDestination.Absolute(attachmentUrl)
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams

    )

    fun addUsers(roomId: Int, users: Array<User>) = addUsers(roomId, users.map { id }.toTypedArray())
    fun addUsers(roomId: Int, userIds: Array<String>) = addOrRemoveUsers("add", roomId, userIds)

    fun removeUsers(roomId: Int, users: Array<User>) = removeUsers(roomId, users.map { id }.toTypedArray())
    fun removeUsers(roomId: Int, userIds: Array<String>) = addOrRemoveUsers("remove", roomId, userIds)

    private fun addOrRemoveUsers(
        operation: String,
        roomId: Int,
        userIds: Array<String>
    ): Future<Result<Boolean, Error>> = apiInstance.request<String>(
        options = RequestOptions(
            method = "PUT",
            path = "/rooms/$roomId/users/$operation",
            body = GSON.toJson(object { val userIds = userIds })
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).mapResult { true }

    /**
     * Update a room
     * */
    fun updateRoom(
        room: Room,
        name: String? = null,
        isPrivate: Boolean? = null
    ): Future<Result<Boolean, Error>> {
        val path = "/rooms/${room.id}"
        val data = UpdateRoomRequest(
            name = name ?: room.name,
            isPrivate = isPrivate ?: room.isPrivate
        )

        return apiInstance.request<String>(
            options = RequestOptions(
                method = "PUT",
                path = path,
                body = GSON.toJson(data)
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams
        ).mapResult { true }
    }

    data class UpdateRoomRequest(val name: String, val isPrivate: Boolean)

    /**
     * Delete a room
     * */
    fun deleteRoom(room: Room): Future<Result<Boolean, Error>> =
        deleteRoom(room.id)

    fun deleteRoom(roomId: Int): Future<Result<Boolean, Error>> = apiInstance.request<String>(
        options = RequestOptions(
            method = "DELETE",
            path = "/rooms/$roomId",
            body = ""
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).mapResult { true }

    /**
     * Leave a room
     * */
    fun leaveRoom(room: Room): Future<Result<Boolean, Error>> =
        leaveRoom(room.id)

    fun leaveRoom(roomId: Int): Future<Result<Boolean, Error>> = apiInstance.request<String>(
        options = RequestOptions(
            method = "POST",
            path = "/users/$id/rooms/$roomId/leave",
            body = "" //TODO: this is a horrible OKHTTP hack - POST is required to have a body.
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).mapResult { true }

    @JvmOverloads
    fun subscribeToRoom(room: Room, listeners: RoomSubscriptionListeners, messageLimit : Int = 10) {
        _roomSubscriptions += RoomSubscription(room, id, listeners, chatManager, messageLimit)
    }

    @JvmOverloads
    fun sendMessage(room: Room, messageText: String, attachment: GenericAttachment = NoAttachment): Future<Result<Int, Error>> =
        chatManager.messageService(room).sendMessage(id, messageText, attachment)

    fun close() {
        roomSubscriptions.forEach { it.unsubscribe() }
    }

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
