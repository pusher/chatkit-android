package com.pusher.chatkit

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.files.FetchedAttachment
import com.pusher.chatkit.files.GenericAttachment
import com.pusher.chatkit.files.NoAttachment
import com.pusher.chatkit.messages.Direction
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomConsumer
import com.pusher.chatkit.rooms.RoomListeners
import com.pusher.chatkit.rooms.toCallback
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.users.User
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import elements.Subscription

@Suppress("MemberVisibilityCanBePrivate") // Entry points
class CurrentUser(
    val id: String,
    var avatarURL: String?,
    var customData: CustomData?,
    var name: String?,
    private val chatManager: ChatManager,
    private val client: PlatformClient
) {

    val rooms: List<Room> get() = chatManager.roomService.roomStore.toList()

    val users: Result<List<User>, Error>
        get() = rooms
                .flatMap { it.memberUserIds }
                .let { ids -> chatManager.userService.fetchUsersBy(ids.toSet()) }
                .map { it.values.toList() }

    // TODO get rid
    private val roomSubscriptions = mutableMapOf<Int, ChatkitSubscription>()

    fun isSubscribedToRoom(roomId: Int): Boolean =
            roomSubscriptions.containsKey(roomId)

    fun isSubscribedToRoom(room: Room): Boolean =
            isSubscribedToRoom(room.id)

    fun updateWithPropertiesOf(newUser: CurrentUser) {
        name = newUser.name
        customData = newUser.customData
    }

    fun setReadCursor(room: Room, position: Int) =
            setReadCursor(room.id, position)

    fun setReadCursor(roomId: Int, position: Int) =
            chatManager.cursorService.setReadCursor(id, roomId, position)

    fun getReadCursor(roomId: Int): Result<Cursor, Error> =
            chatManager.cursorService.getReadCursor(id, roomId)

    fun getReadCursor(room: Room): Result<Cursor, Error> =
            getReadCursor(room.id)

    fun fetchAttachment(attachmentUrl: String): Result<FetchedAttachment, Error> =
            chatManager.filesService.fetchAttachment(attachmentUrl)

    fun addUsersToRoom(roomId: Int, userIds: List<String>) =
            chatManager.userService.addUsersToRoom(roomId, userIds)

    fun removeUsersFromRoom(roomId: Int, userIds: List<String>) =
            chatManager.userService.removeUsersFromRoom(roomId, userIds)

    @JvmOverloads
    fun createRoom(
            name: String,
            isPrivate: Boolean = false,
            userIds: List<String> = emptyList()
    ): Result<Room, Error> = chatManager.roomService.createRoom(
            creatorId = id,
            name = name,
            isPrivate = isPrivate,
            userIds = userIds
    )

    @JvmOverloads
    fun updateRoom(room: Room, name: String, isPrivate: Boolean? = null): Result<Unit, Error> =
            updateRoom(room.id, name, isPrivate)

    @JvmOverloads
    fun updateRoom(roomId: Int, name: String, isPrivate: Boolean? = null): Result<Unit, Error> =
            chatManager.roomService.updateRoom(roomId, name, isPrivate)

    fun deleteRoom(room: Room): Result<Int, Error> =
            deleteRoom(room.id)

    fun deleteRoom(roomId: Int): Result<Int, Error> =
            chatManager.roomService.deleteRoom(roomId)

    fun leaveRoom(room: Room): Result<Int, Error> =
            leaveRoom(room.id)

    fun leaveRoom(roomId: Int): Result<Int, Error> =
            chatManager.roomService.leaveRoom(id, roomId)

    fun joinRoom(room: Room): Result<Room, Error> =
            joinRoom(room.id)

    fun joinRoom(roomId: Int): Result<Room, Error> =
            chatManager.roomService.joinRoom(id, roomId)

    @JvmOverloads
    fun subscribeToRoom(
            room: Room,
            listeners: RoomListeners,
            messageLimit: Int = 10
    ): ChatkitSubscription =
            subscribeToRoom(room.id, listeners, messageLimit)

    @JvmOverloads
    fun subscribeToRoom(
            roomId: Int,
            listeners: RoomListeners,
            messageLimit: Int = 10
    ): ChatkitSubscription =
            subscribeToRoom(roomId, messageLimit, listeners.toCallback())

    @JvmOverloads
    fun subscribeToRoom(
            room: Room,
            messageLimit: Int = 10,
            consumer: RoomConsumer
    ): ChatkitSubscription =
            subscribeToRoom(room.id, messageLimit, consumer)

    @JvmOverloads
    fun subscribeToRoom(
            roomId: Int,
            messageLimit: Int = 10,
            consumer: RoomConsumer
    ) = chatManager.roomService.subscribeToRoom(roomId, consumer, messageLimit)
            .autoRemove(roomId)
            .also { roomSubscriptions += roomId to it }

    private fun Subscription.autoRemove(roomId: Int) = object : ChatkitSubscription {
        override fun connect(): ChatkitSubscription {
            return this
        }

        override fun unsubscribe() {
            roomSubscriptions -= roomId
            this@autoRemove.unsubscribe()
        }
    }

    @JvmOverloads
    fun fetchMessages(
            roomId: Int,
            initialId: Int? = null,
            direction: Direction = Direction.OLDER_FIRST,
            limit: Int = 10
    ): Result<List<Message>, Error> = chatManager
            .messageService
            .fetchMessages(roomId, limit, initialId, direction)

    @JvmOverloads
    fun sendMessage(
            room: Room,
            messageText: String,
            attachment: GenericAttachment = NoAttachment
    ): Result<Int, Error> =
            sendMessage(room.id, messageText, attachment)

    @JvmOverloads
    fun sendMessage(
            roomId: Int,
            messageText: String,
            attachment: GenericAttachment = NoAttachment
    ): Result<Int, Error> =
            chatManager.messageService.sendMessage(roomId, id, messageText, attachment)

    private var lastTypingEvent: Long = 0

    private fun canSendTypingEvent() =
            (System.currentTimeMillis() - lastTypingEvent) > TYPING_TIME_THRESHOLD

    fun isTypingIn(room: Room): Result<Unit, Error> =
            isTypingIn(room.id)

    fun isTypingIn(roomId: Int): Result<Unit, Error> =
            if (canSendTypingEvent()) {
                lastTypingEvent = System.currentTimeMillis()
                client.doPost(
                        path = "/rooms/$roomId/typing_indicators"
                )
            } else {
                Unit.asSuccess()
            }

    fun getJoinableRooms(): Result<List<Room>, Error> =
            chatManager.roomService.fetchUserRooms(
                    userId = id,
                    joinable = true
            )

    fun usersForRoom(room: Room): Result<List<User>, Error> =
            chatManager.userService.fetchUsersBy(room.memberUserIds)
                    .map { it.values.toList() }

    fun close() {
        for (roomSub in roomSubscriptions.values) {
            roomSub.unsubscribe()
        }
        roomSubscriptions.clear()
    }

}

private const val TYPING_TIME_THRESHOLD = 500
