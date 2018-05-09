package com.pusher.chatkit

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.messages.*
import com.pusher.chatkit.network.parseAs
import com.pusher.chatkit.network.toJson
import com.pusher.chatkit.rooms.*
import com.pusher.chatkit.users.User
import com.pusher.platform.Instance
import com.pusher.platform.RequestDestination
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.toFuture
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.Error
import elements.Subscription
import java.util.concurrent.Future

class CurrentUser(
    val id: String,
    val filesInstance: Instance,
    val tokenParams: ChatkitTokenParams?,
    val tokenProvider: TokenProvider,
    var avatarURL: String?,
    var customData: CustomData?,
    var name: String?,
    private val chatManager: ChatManager
) {

    val rooms: List<Room> get() = chatManager.roomStore.toList()
        .filter { it.memberUserIds.contains(id) }
    val users: Future<Result<List<User>, Error>>
        get() = rooms
        .flatMap { it.memberUserIds }
        .let { ids -> chatManager.userService.fetchUsersBy(ids.toSet()) }

    private val roomSubscriptions = mutableMapOf<Int, Subscription>()

    internal fun isSubscribedToRoom(roomId: Int): Boolean =
        roomSubscriptions.containsKey(roomId)

    fun updateWithPropertiesOf(newUser: User) {
        name = newUser.name
        customData = newUser.customData
    }

    fun setReadCursor(room: Room, position: Int): Future<Result<Boolean, Error>> =
        setReadCursor(room.id, position)

    fun setReadCursor(roomId: Int, position: Int): Future<Result<Boolean, Error>> =
        chatManager.cursorService.setReadCursor(id, roomId, position)

    fun getReadCursor(roomId: Int) : Result<Cursor, Error> =
        chatManager.cursorService.getReadCursor(id, roomId)

    fun getReadCursor(room: Room) : Result<Cursor, Error> =
        getReadCursor(room.id)

    fun fetchAttachment(attachmentUrl: String): Future<Result<FetchedAttachment, Error>> =
        filesInstance.request(
            options = RequestOptions(
                method = "GET",
                destination = RequestDestination.Absolute(attachmentUrl)
            ),
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            responseParser = { it.parseAs<FetchedAttachment>() }
        )

    fun addUsersToRoom(roomId: Int, userIds: List<String>) =
        chatManager.userService.addUsersToRoom(roomId, userIds)

    fun removeUsersFromRoom(roomId: Int, userIds: List<String>) =
        chatManager.userService.removeUsersFromRoom(roomId, userIds)

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
        subscribeToRoom(room.id, messageLimit, listeners.toCallback())

    @JvmOverloads
    fun subscribeToRoom(
        roomId: Int,
        listeners: RoomSubscriptionListeners,
        messageLimit : Int = 10
    ): Subscription =
        subscribeToRoom(roomId, messageLimit, listeners.toCallback())

    @JvmOverloads
    fun subscribeToRoom(
        room: Room,
        messageLimit : Int = 10,
        consumer: RoomSubscriptionConsumer
    ): Subscription =
        subscribeToRoom(room.id, messageLimit, consumer)

    @JvmOverloads
    fun subscribeToRoom(
        roomId: Int,
        messageLimit : Int = 10,
        consumer: RoomSubscriptionConsumer
    ): Subscription =
        chatManager.roomService().subscribeToRoom(id, roomId, consumer, messageLimit)
            .also { roomSubscriptions += roomId to it }

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

    private var lastTypingEvent: Long = 0

    private fun canSendTypingEvent() =
        (System.currentTimeMillis() - lastTypingEvent) > TYPING_TIME_THRESHOLD

    fun isTypingIn(room: Room): Future<Result<Unit, Error>> =
        isTypingIn(room.id)

    fun isTypingIn(roomId: Int): Future<Result<Unit, Error>> =
        TypingIndicatorBody(id).toJson().toFuture()
            .takeIf { canSendTypingEvent() }
            ?.flatMapFutureResult { body ->
                chatManager.doPost<Unit>("/rooms/$roomId/events", body)
                    .also { lastTypingEvent = System.currentTimeMillis() }
            } ?: Unit.asSuccess<Unit, Error>().toFuture()

    internal data class TypingIndicatorBody(
        val userId: String,
        val name: String = "typing_start"
    )

    fun getJoinablerooms(): Future<Result<List<Room>, Error>> =
        chatManager.roomService().fetchUserRooms(
            userId = id,
            onlyJoinable = true
        )

    fun usersForRoom(room: Room): Future<Result<List<User>, Error>> =
        chatManager.userService.fetchUsersBy(room.memberUserIds)

    fun close() {
        roomSubscriptions.values.forEach { it.unsubscribe() }
        roomSubscriptions.clear()
    }

}

internal data class MessageRequest(val text: String? = null, val userId: String, val attachment: AttachmentBody? = null)

internal sealed class AttachmentBody {
    data class Resource(val resourceLink: String, val type: String) : AttachmentBody()
    object None : AttachmentBody()
    data class Failed(val error: Error) : AttachmentBody()
}

internal data class SetCursorRequest(val position: Int)

internal data class RoomCreateRequest(
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

private const val TYPING_TIME_THRESHOLD = 500
