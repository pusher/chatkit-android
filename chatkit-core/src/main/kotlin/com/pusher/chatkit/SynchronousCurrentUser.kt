package com.pusher.chatkit

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.files.GenericAttachment
import com.pusher.chatkit.files.NoAttachment
import com.pusher.chatkit.messages.Direction
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.pushnotifications.PushNotifications
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomConsumer
import com.pusher.chatkit.rooms.RoomListeners
import com.pusher.chatkit.rooms.toCallback
import com.pusher.chatkit.users.User
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import elements.Subscription
import java.net.URLEncoder

@Suppress("MemberVisibilityCanBePrivate") // Entry points
class SynchronousCurrentUser(
        val id: String,
        var avatarURL: String?,
        var customData: CustomData?,
        var name: String?,
        private val chatManager: SynchronousChatManager,
        private val client: PlatformClient,
        private val pushNotifications: PushNotifications?
) {

    val rooms: List<Room> get() = chatManager.roomService.roomStore.toList()

    val users: Result<List<User>, Error>
        get() = rooms
                .flatMap { it.memberUserIds }
                .let { ids -> chatManager.userService.fetchUsersBy(ids.toSet()) }
                .map { it.values.toList() }

    fun isSubscribedToRoom(roomId: String): Boolean =
            chatManager.roomService.isSubscribedTo(roomId)

    fun isSubscribedToRoom(room: Room): Boolean =
            isSubscribedToRoom(room.id)

    fun updateWithPropertiesOf(newUser: SynchronousCurrentUser) {
        name = newUser.name
        customData = newUser.customData
    }

    fun setReadCursor(room: Room, position: Int) =
            setReadCursor(room.id, position)

    fun setReadCursor(roomId: String, position: Int) =
            chatManager.cursorService.setReadCursor(id, roomId, position)

    fun getReadCursor(roomId: String): Result<Cursor, Error> =
            chatManager.cursorService.getReadCursor(id, roomId)

    fun getReadCursor(room: Room): Result<Cursor, Error> =
            getReadCursor(room.id)

    fun addUsersToRoom(roomId: String, userIds: List<String>) =
            chatManager.userService.addUsersToRoom(roomId, userIds)

    fun removeUsersFromRoom(roomId: String, userIds: List<String>) =
            chatManager.userService.removeUsersFromRoom(roomId, userIds)

    @JvmOverloads
    fun createRoom(
            name: String,
            isPrivate: Boolean = false,
            customData: CustomData? = null,
            userIds: List<String> = emptyList()
    ): Result<Room, Error> = chatManager.roomService.createRoom(
            creatorId = id,
            name = name,
            isPrivate = isPrivate,
            customData = customData,
            userIds = userIds
    )

    @JvmOverloads
    fun updateRoom(room: Room, name: String? = null, isPrivate: Boolean? = null, customData: CustomData? = null): Result<Unit, Error> =
            this.updateRoom(room.id, name, isPrivate, customData)

    @JvmOverloads
    fun updateRoom(roomId: String, name: String? = null, isPrivate: Boolean? = null, customData: CustomData? = null): Result<Unit, Error> =
            chatManager.roomService.updateRoom(roomId, name, isPrivate, customData)

    fun deleteRoom(room: Room): Result<String, Error> =
            deleteRoom(room.id)

    fun deleteRoom(roomId: String): Result<String, Error> =
            chatManager.roomService.deleteRoom(roomId)

    fun leaveRoom(room: Room): Result<String, Error> =
            leaveRoom(room.id)

    fun leaveRoom(roomId: String): Result<String, Error> =
            chatManager.roomService.leaveRoom(id, roomId)

    fun joinRoom(room: Room): Result<Room, Error> =
            joinRoom(room.id)

    fun joinRoom(roomId: String): Result<Room, Error> =
            chatManager.roomService.joinRoom(id, roomId)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            room: Room,
            listeners: RoomListeners,
            messageLimit: Int = 10
    ): Subscription =
            subscribeToRoom(room.id, listeners, messageLimit)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            roomId: String,
            listeners: RoomListeners,
            messageLimit: Int = 10
    ): Subscription =
            subscribeToRoom(roomId, messageLimit, listeners.toCallback())

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            room: Room,
            messageLimit: Int = 10,
            consumer: RoomConsumer
    ): Subscription =
            subscribeToRoom(room.id, messageLimit, consumer)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            roomId: String,
            messageLimit: Int = 10,
            consumer: RoomConsumer
    ): Subscription =
            chatManager.roomService.subscribeToRoom(roomId, consumer, messageLimit)

    @JvmOverloads
    fun subscribeToRoomMultipart(
            room: Room,
            listeners: RoomListeners,
            messageLimit: Int = 10
    ): Subscription =
            subscribeToRoomMultipart(room.id, listeners, messageLimit)

    @JvmOverloads
    fun subscribeToRoomMultipart(
            roomId: String,
            listeners: RoomListeners,
            messageLimit: Int = 10
    ): Subscription =
            subscribeToRoomMultipart(roomId, messageLimit, listeners.toCallback())

    @JvmOverloads
    fun subscribeToRoomMultipart(
            room: Room,
            messageLimit: Int = 10,
            consumer: RoomConsumer
    ): Subscription =
            subscribeToRoomMultipart(room.id, messageLimit, consumer)

    @JvmOverloads
    fun subscribeToRoomMultipart(
            roomId: String,
            messageLimit: Int = 10,
            consumer: RoomConsumer
    ): Subscription =
            chatManager.roomService.subscribeToRoomMultipart(roomId, consumer, messageLimit)

    @JvmOverloads
    @Deprecated("use fetchMultipartMessages")
    fun fetchMessages(
            roomId: String,
            initialId: Int? = null,
            direction: Direction = Direction.OLDER_FIRST,
            limit: Int = 10
    ): Result<List<Message>, Error> = chatManager
            .messageService
            .fetchMessages(roomId, limit, initialId, direction)

    @JvmOverloads
    fun fetchMultipartMessages(
            roomId: String,
            initialId: Int? = null,
            direction: Direction = Direction.OLDER_FIRST,
            limit: Int = 10
    ): Result<List<com.pusher.chatkit.messages.multipart.Message>, Error> = chatManager
            .messageService
            .fetchMultipartMessages(roomId, limit, initialId, direction)

    @JvmOverloads
    @Deprecated("use sendSimpleMessage or sendMultipartMessage")
    fun sendMessage(
            room: Room,
            messageText: String,
            attachment: GenericAttachment = NoAttachment
    ): Result<Int, Error> =
            sendMessage(room.id, messageText, attachment)

    @JvmOverloads
    @Deprecated("use sendSimpleMessage or sendMultipartMessage")
    fun sendMessage(
            roomId: String,
            messageText: String,
            attachment: GenericAttachment = NoAttachment
    ): Result<Int, Error> =
            chatManager.messageService.sendMessage(roomId, id, messageText, attachment)

    fun sendSimpleMessage(
            room: Room,
            messageText: String
    ): Result<Int, Error> =
            sendSimpleMessage(room.id, messageText)

    fun sendSimpleMessage(
            roomId: String,
            messageText: String
    ): Result<Int, Error> =
            sendMultipartMessage(roomId, listOf(NewPart.Inline(messageText)))

    fun sendMultipartMessage(
            room: Room,
            parts: List<NewPart>
    ): Result<Int, Error> =
            sendMultipartMessage(room.id, parts)

    fun sendMultipartMessage(
            roomId: String,
            parts: List<NewPart>
    ): Result<Int, Error> =
            chatManager.messageService.sendMultipartMessage(roomId, parts)

    private val typingTimeThreshold = 500
    private var lastTypingEvent: Long = 0

    private fun canSendTypingEvent() =
            (System.currentTimeMillis() - lastTypingEvent) > typingTimeThreshold

    fun isTypingIn(room: Room): Result<Unit, Error> =
            isTypingIn(room.id)

    fun isTypingIn(roomId: String): Result<Unit, Error> =
            if (canSendTypingEvent()) {
                lastTypingEvent = System.currentTimeMillis()
                client.doPost(
                        path = "/rooms/${URLEncoder.encode(roomId, "UTF-8")}/typing_indicators"
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

    fun enablePushNotifications(): Result<Unit, Error> {
        if (pushNotifications != null) {
            return pushNotifications.start().flatMap {
                pushNotifications.setUserId(id)
            }
        } else {
            throw IllegalStateException("Push Notifications dependency is not available. " +
                    "Did you provide a Context to AndroidChatkitDependencies?")
        }
    }
}
