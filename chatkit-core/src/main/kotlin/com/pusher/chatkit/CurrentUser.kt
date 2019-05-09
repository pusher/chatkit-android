package com.pusher.chatkit

import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.files.GenericAttachment
import com.pusher.chatkit.files.NoAttachment
import com.pusher.chatkit.messages.Direction
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomConsumer
import com.pusher.chatkit.rooms.RoomListeners
import com.pusher.chatkit.users.User
import com.pusher.platform.network.Futures
import com.pusher.util.Result
import elements.Error
import elements.Subscription

@Suppress("MemberVisibilityCanBePrivate") // Entry points
class CurrentUser(
        private val syncCurrentUser: SynchronousCurrentUser
) {
    //Delegate fields for the underlying SynchronousCurrentUsers
    val id: String
        get() = syncCurrentUser.id

    val name: String?
        get() = syncCurrentUser.name

    val avatarURL: String?
        get() = syncCurrentUser.avatarURL

    val customData: CustomData?
        get() = syncCurrentUser.customData

    val rooms
        get() = syncCurrentUser.rooms


    fun users(callback: (Result<List<User>, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.users }, callback)

    fun isSubscribedToRoom(roomId: String): Boolean =
            syncCurrentUser.isSubscribedToRoom(roomId)

    fun isSubscribedToRoom(room: Room): Boolean =
            syncCurrentUser.isSubscribedToRoom(room)

    fun updateWithPropertiesOf(newUser: CurrentUser) =
            syncCurrentUser.updateWithPropertiesOf(newUser.syncCurrentUser)

    fun setReadCursor(room: Room, position: Int) {
        Futures.schedule { syncCurrentUser.setReadCursor(room.id, position) }
    }

    fun setReadCursor(roomId: String, position: Int) {
        Futures.schedule { syncCurrentUser.setReadCursor(roomId, position) }
    }

    fun getReadCursor(roomId: String): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(roomId)

    fun getReadCursor(room: Room): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(room.id)

    fun getReadCursor(roomId: String, userId: String): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(roomId, userId)

    fun getReadCursor(room: Room, user: User): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(room.id, user.id)

    fun addUsersToRoom(roomId: String, userIds: List<String>, callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.addUsersToRoom(roomId, userIds) }, callback)

    fun removeUsersFromRoom(
            roomId: String,
            userIds: List<String>,
            callback: (Result<Unit, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.removeUsersFromRoom(roomId, userIds) }, callback)

    @JvmOverloads
    fun createRoom(
            name: String,
            isPrivate: Boolean = false,
            customData: CustomData? = null,
            userIds: List<String> = emptyList(),
            callback: (Result<Room, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.createRoom(name, isPrivate, customData, userIds) }, callback)

    @JvmOverloads
    fun updateRoom(
            room: Room,
            name: String,
            isPrivate: Boolean? = null,
            customData: CustomData? = null,
            callback: (Result<Unit, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.updateRoom(room, name, isPrivate, customData) }, callback)

    @JvmOverloads
    fun updateRoom(roomId: String,
                   name: String,
                   isPrivate: Boolean? = null,
                   customData: CustomData? = null,
                   callback: (Result<Unit, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.updateRoom(roomId, name, isPrivate, customData) }, callback)

    fun deleteRoom(room: Room, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.deleteRoom(room) }, callback)

    fun deleteRoom(roomId: String, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.deleteRoom(roomId) }, callback)

    fun leaveRoom(room: Room, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.leaveRoom(room) }, callback)

    fun leaveRoom(roomId: String, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.leaveRoom(roomId) }, callback)

    fun joinRoom(room: Room, callback: (Result<Room, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.joinRoom(room) }, callback)

    fun joinRoom(roomId: String, callback: (Result<Room, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.joinRoom(roomId) }, callback)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            room: Room,
            listeners: RoomListeners,
            messageLimit: Int = 10,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoom(room, listeners, messageLimit) }, callback)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            roomId: String,
            listeners: RoomListeners,
            messageLimit: Int = 10,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoom(roomId, listeners, messageLimit) }, callback)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            room: Room,
            messageLimit: Int = 10,
            consumer: RoomConsumer,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoom(room, messageLimit, consumer) }, callback)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoom(
            roomId: String,
            messageLimit: Int = 10,
            consumer: RoomConsumer,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoom(roomId, messageLimit, consumer) }, callback)

    @JvmOverloads
    fun subscribeToRoomMultipart(
            room: Room,
            listeners: RoomListeners,
            messageLimit: Int = 10,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoomMultipart(room, listeners, messageLimit) }, callback)

    @JvmOverloads
    fun subscribeToRoomMultipart(
            roomId: String,
            listeners: RoomListeners,
            messageLimit: Int = 10,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoomMultipart(roomId, listeners, messageLimit) }, callback)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoomMultipart(
            room: Room,
            messageLimit: Int = 10,
            consumer: RoomConsumer,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoomMultipart(room, messageLimit, consumer) }, callback)

    @JvmOverloads
    @Deprecated("use subscribeToRoomMultipart")
    fun subscribeToRoomMultipart(
            roomId: String,
            messageLimit: Int = 10,
            consumer: RoomConsumer,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoomMultipart(roomId, messageLimit, consumer) }, callback)
    
    @JvmOverloads
    @Deprecated("use fetchMultipartMessages")
    fun fetchMessages(
            roomId: String,
            initialId: Int? = null,
            direction: Direction = Direction.OLDER_FIRST,
            limit: Int = 10,
            callback: (Result<List<Message>, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.fetchMessages(roomId, initialId, direction, limit) }, callback)

    @JvmOverloads
    fun fetchMultipartMessages(
            roomId: String,
            initialId: Int? = null,
            direction: Direction = Direction.OLDER_FIRST,
            limit: Int = 10,
            callback: (Result<List<com.pusher.chatkit.messages.multipart.Message>, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.fetchMultipartMessages(roomId, initialId, direction, limit) }, callback)

    @JvmOverloads
    @Deprecated("use sendSimpleMessage or sendMultipartMessage")
    fun sendMessage(
            room: Room,
            messageText: String,
            attachment: GenericAttachment = NoAttachment,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendMessage(room, messageText, attachment) }, callback)

    @JvmOverloads
    @Deprecated("use sendSimpleMessage or sendMultipartMessage")
    fun sendMessage(
            roomId: String,
            messageText: String,
            attachment: GenericAttachment = NoAttachment,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendMessage(roomId, messageText, attachment) }, callback)

    fun sendSimpleMessage(
            roomId: String,
            messageText: String,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendSimpleMessage(roomId, messageText) }, callback)

    fun sendSimpleMessage(
            room: Room,
            messageText: String,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendSimpleMessage(room, messageText) }, callback)

    fun sendMultipartMessage(
            room: Room,
            parts: List<NewPart>,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendMultipartMessage(room, parts) }, callback)

    fun sendMultipartMessage(
            roomId: String,
            parts: List<NewPart>,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendMultipartMessage(roomId, parts) }, callback)

    fun isTypingIn(room: Room, callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.isTypingIn(room) }, callback)

    fun isTypingIn(roomId: String, callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.isTypingIn(roomId) }, callback)

    fun getJoinableRooms(callback: (Result<List<Room>, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.getJoinableRooms() }, callback)

    fun usersForRoom(room: Room, callback: (Result<List<User>, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.usersForRoom(room) }, callback)

    fun enablePushNotifications(callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.enablePushNotifications() }, callback)
}
