package com.pusher.chatkit

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.ChatManager.Companion.GSON
import com.pusher.chatkit.messages.Direction
import com.pusher.chatkit.messages.messageService
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.toJson
import com.pusher.platform.Instance
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.toFuture
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.flatMapFutureResult
import com.pusher.util.mapResult
import elements.Error
import elements.Subscription
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

    val roomSubscriptions: List<Subscription>
        get() = _roomSubscriptions

    private val _roomSubscriptions = mutableListOf<Subscription>()

    fun updateWithPropertiesOf(newUser: User) {
        updatedAt = newUser.updatedAt
        name = newUser.name
        customData = newUser.customData
        // TODO reopen subscriptions
    }

    fun presence(consumeEvent: (ChatManagerEvent) -> Unit) = PresenceSubscription(
        instance = presenceInstance,
        path = "/users/$id/presence",
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        chatManager = chatManager,
        consumeEvent = consumeEvent
    )

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
        tokenParams = tokenParams,
        responseParser = { it.parseAs() }
    ).mapResult { true }

    fun fetchAttachment(attachmentUrl: String) = filesInstance.request<FetchedAttachment>(
        options = RequestOptions(
            method = "GET",
            destination = RequestDestination.Absolute(attachmentUrl)
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        responseParser = { it.parseAs() }
    )

    fun addUsersToRoom(roomId: Int, userIds: List<String>) =
        chatManager.userService().addUsersToRoom(roomId, userIds)

    fun removeUsersFromRoom(roomId: Int, userIds: List<String>) =
        chatManager.userService().removeUsersFromRoom(roomId, userIds)

    @JvmOverloads
    fun createRoom(
        name: String,
        isPrivate: Boolean = false,
        userIds: List<String> = emptyList()
    ): Future<Result<Room, Error>> = chatManager.roomService().createRoom(
        creatorId = id,
        name = name,
        isPrivate = isPrivate,
        userIds = userIds
    )

    @JvmOverloads
    fun updateRoom(room: Room, name: String, isPrivate: Boolean? = null): Future<Result<Unit, Error>> =
        updateRoom(room.id, name, isPrivate)

    @JvmOverloads
    fun updateRoom(roomId: Int, name: String, isPrivate: Boolean? = null): Future<Result<Unit, Error>> =
        chatManager.roomService().updateRoom(roomId, name, isPrivate)

    fun deleteRoom(room: Room): Future<Result<String, Error>> =
        deleteRoom(room.id)

    fun deleteRoom(roomId: Int): Future<Result<String, Error>> =
        chatManager.roomService().deleteRoom(roomId)

    fun leaveRoom(room: Room): Future<Result<Unit, Error>> =
        leaveRoom(room.id)

    fun leaveRoom(roomId: Int): Future<Result<Unit, Error>> =
        chatManager.roomService().leaveRoom(id, roomId)

    fun joinRoom(room: Room): Future<Result<Room, Error>> =
        joinRoom(room.id)

    fun joinRoom(roomId: Int): Future<Result<Room, Error>> =
        chatManager.roomService().joinRoom(id, roomId)

    @JvmOverloads
    fun subscribeToRoom(
        room: Room,
        listeners: RoomSubscriptionListeners,
        messageLimit : Int = 10
    ): Subscription =
        subscribeToRoom(room.id, listeners, messageLimit)

    @JvmOverloads
    fun subscribeToRoom(
        roomId: Int,
        listeners: RoomSubscriptionListeners,
        messageLimit : Int = 10
    ): Subscription =
        chatManager.roomService().subscribeToRoom(id, roomId, listeners, messageLimit)
            .also { _roomSubscriptions += it }

    @JvmOverloads
    fun fetchMessages(
        room: Room,
        initialId: Int? = null,
        direction: Direction = Direction.ORDER_FIRST,
        limit: Int = 10
    ): Future<Result<List<Message>, Error>> =
        fetchMessages(room.id, initialId, direction, limit)

    @JvmOverloads
    fun fetchMessages(
        roomId: Int,
        initialId: Int? = null,
        direction: Direction = Direction.ORDER_FIRST,
        limit: Int = 10
    ): Future<Result<List<Message>, Error>> = chatManager
        .messageService(roomId)
        .fetchMessages(limit, initialId, direction)

    @JvmOverloads
    fun sendMessage(
        room: Room,
        messageText: String,
        attachment: GenericAttachment = NoAttachment
    ): Future<Result<Int, Error>> =
        sendMessage(room.id, messageText, attachment)

    @JvmOverloads
    fun sendMessage(
        roomId: Int,
        messageText: String,
        attachment: GenericAttachment = NoAttachment
    ): Future<Result<Int, Error>> =
        chatManager.messageService(roomId).sendMessage(id, messageText, attachment)

    fun getJoinablerooms() =
        chatManager.roomService().fetchUserRooms(
            userId = id,
            onlyJoinable = true
        )

    fun close() {
        roomSubscriptions.forEach { it.unsubscribe() }
    }

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
