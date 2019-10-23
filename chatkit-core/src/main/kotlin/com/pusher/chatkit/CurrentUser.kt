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
import com.pusher.chatkit.rooms.RoomPushNotificationTitle
import com.pusher.chatkit.users.User
import com.pusher.platform.network.Futures
import com.pusher.util.Result
import elements.Error
import elements.Subscription

/**
 * The interface for what the current user can do in Chatkit asynchronously using callbacks.
 */
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


    /**
     * Returns a callback with a list of all the users on the instance if successful.
     * If unsuccessful an [Error] will be returned.
     */
    fun users(callback: (Result<List<User>, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.users }, callback)

    /**
     * Returns whether the current user is subscribed to the [roomId]
     */
    fun isSubscribedToRoom(roomId: String): Boolean =
            syncCurrentUser.isSubscribedToRoom(roomId)

    /**
     * @see [isSubscribedToRoom]
     */
    fun isSubscribedToRoom(room: Room): Boolean =
            syncCurrentUser.isSubscribedToRoom(room)

    /**
     * Updates the current user with the properties of [newUser]
     */
    fun updateWithPropertiesOf(newUser: CurrentUser) =
            syncCurrentUser.updateWithPropertiesOf(newUser.syncCurrentUser)

    /**
     * Sets the read cursor for the cur rent user in [room] to [position].
     * The [position] is the id of the last message that the current user has read.
     */
    fun setReadCursor(room: Room, position: Int) {
        Futures.schedule { syncCurrentUser.setReadCursor(room.id, position) }
    }

    /**
     * @see [setReadCursor]
     */
    fun setReadCursor(roomId: String, position: Int) {
        Futures.schedule { syncCurrentUser.setReadCursor(roomId, position) }
    }

    /**
     * Returns the read [Cursor] for the current user in [roomId] if successful If unsuccessful
     * an [Error] is returned.
     */
    fun getReadCursor(roomId: String): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(roomId)
    /**
     * @see [getReadCursor]
     */
    fun getReadCursor(room: Room): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(room.id)

    /**
     * Returns the read [Cursor] for the [userId] in [roomId] if successful If unsuccessful
     * an [Error] is returned.
     */
    fun getReadCursor(roomId: String, userId: String): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(roomId, userId)

    /**
     * @see [getReadCursor]
     */
    fun getReadCursor(room: Room, user: User): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(room.id, user.id)

    /**
     * Adds the [userIds] to the [roomId]. The [callback] returns void if successful,
     * and an [Error] if unsuccessful.
     */
    fun addUsersToRoom(roomId: String, userIds: List<String>, callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.addUsersToRoom(roomId, userIds) }, callback)

    /**
     * Removes the [userIds] from the [roomId]. The [callback] returns void if successful,
     * and an [Error] if unsuccessful.
     */
    fun removeUsersFromRoom(
            roomId: String,
            userIds: List<String>,
            callback: (Result<Unit, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.removeUsersFromRoom(roomId, userIds) }, callback)

    /**
     * Create a new room with:
     * - [id] - optional - a uniquely identifying String, if not provided a randomly generated unique string will be created for you
     * - [name] - a String to label the room
     * - [pushNotificationTitleOverride] - optional - a string that appears on the push notification title for this room, by default the title is the room name
     * - [isPrivate] - optional - a boolean to determine if the room is available for others to join, by default the room will be not be private
     * - [customData] - optional - a [CustomData] object with any extra information you'd like to save with the new room e.g. mapOf("description" to "some extra description about the room")
     * - [userIds] - optional - a list of the userIds of users who will be added to the room
     * - [callback] - returns a [Result] with the [Room] if success, or an [Error] if something went wrong
     */
    @JvmOverloads
    fun createRoom(
            id: String? = null,
            name: String,
            pushNotificationTitleOverride: String? = null,
            isPrivate: Boolean = false,
            customData: CustomData? = null,
            userIds: List<String> = emptyList(),
            callback: (Result<Room, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.createRoom(id, name, pushNotificationTitleOverride, isPrivate, customData, userIds) }, callback)

    /**
     * Update an existing [room]'s properties:
     * - [name] - a String to label the room
     * - [pushNotificationTitleOverride] - optional - a string that appears on the push notification title for this room, by default the title is the room name
     * - [isPrivate]- optional - a boolean to determine if the room is available for others to join, by default the room will be not be private
     * - [customData] - optional - a [CustomData] object with any extra information you'd like to save with the new room e.g. mapOf("description" to "some extra description about the room")
     * - [callback] - returns a [Result] with the [Room] if success, or an [Error] if something went wrong
     */
    @JvmOverloads
    fun updateRoom(
            room: Room,
            name: String,
            pushNotificationTitleOverride: RoomPushNotificationTitle? = null,
            isPrivate: Boolean? = null,
            customData: CustomData? = null,
            callback: (Result<Unit, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.updateRoom(room, name, pushNotificationTitleOverride, isPrivate, customData) }, callback)

    /**
     * @see [updateRoom]
     */
    @JvmOverloads
    fun updateRoom(roomId: String,
                   name: String,
                   pushNotificationTitleOverride: RoomPushNotificationTitle? = null,
                   isPrivate: Boolean? = null,
                   customData: CustomData? = null,
                   callback: (Result<Unit, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.updateRoom(roomId, name, pushNotificationTitleOverride, isPrivate, customData) }, callback)

    /**
     * Delete the [room]. The [callback] will be called with a [Result] when the operation is complete.
     * If the operation was successful you will receive a [String] which contains ?,
     * or an [Error] informing you of what went wrong.
     * This is a soft delete, which means you cannot reuse the roomId later.
     */
    fun deleteRoom(room: Room, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.deleteRoom(room) }, callback)

    /**
     * @see [deleteRoom]
     */
    fun deleteRoom(roomId: String, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.deleteRoom(roomId) }, callback)

    fun leaveRoom(room: Room, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.leaveRoom(room) }, callback)

    /**
     * @see [leaveRoom]
     */
    fun leaveRoom(roomId: String, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.leaveRoom(roomId) }, callback)

    fun joinRoom(room: Room, callback: (Result<Room, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.joinRoom(room) }, callback)

    /**
     * @see [joinRoom]
     */
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

    /**
     * Subscribe to a [room] for multipart messages. You need to configure your [listners] to listen
     * for the events you're interested in, see [RoomListeners] for more information on what events
     * are possible. You can configure a [messageLimit] which returns 10 messages by default when you
     * subsribe to the room. Finally the [callback] will be called with a [Subscription] if the operation
     * was successful.
     */
    @JvmOverloads
    fun subscribeToRoomMultipart(
            room: Room,
            listeners: RoomListeners,
            messageLimit: Int = 10,
            callback: (Subscription) -> Unit
    ) = makeSingleCallback({ syncCurrentUser.subscribeToRoomMultipart(room, listeners, messageLimit) }, callback)

    /**
     * @see [subscribeToRoomMultipart]
     */
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

    /**
     * Fetches multipart messages for a [roomId] from an optional [initialId]. You can specify the direction
     * of messages you want to receive, by default this is [Direction.OLDER_FIRST] which means you get the older messages
     * through first. However you can alternatively request the messages in [Direction.NEWER_FIRST] which means
     * you'd get the newer messages first. You can specify how many messages to receive back in the [limit].
     * The [callback] will be called with a [Result] when the operation is complete. If the operation
     * was successful you will receive an [List<com.pusher.chatkit.messages.multipart.Message>]
     * which contains the message id, or an [Error] informing you of what went wrong.
     */
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

    /**
     * Send a [messageText] to the [roomId]. The [callback] will be called with a [Result] when the
     * operation is complete. If the operation was successful you will receive an [Int] which
     * contains the message id, or an [Error] informing you of what went wrong.
     */
    fun sendSimpleMessage(
            roomId: String,
            messageText: String,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendSimpleMessage(roomId, messageText) }, callback)

    /**
     * @see [sendSimpleMessage]
     */
    fun sendSimpleMessage(
            room: Room,
            messageText: String,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendSimpleMessage(room, messageText) }, callback)

    /**
     * Sends some message [parts] to the [room]. For more information on what the [parts] can be check
     * out the [docs][https://pusher.com/docs/chatkit/reference/android].
     * The [callback] will be called with a [Result] when the operation is complete. If the
     * operation was successful you will receive an [Int] which contains the message id, or an
     * [Error] informing you of what went wrong.
     */
    fun sendMultipartMessage(
            room: Room,
            parts: List<NewPart>,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendMultipartMessage(room, parts) }, callback)

    /**
     * @see [sendMultipartMessage]
     */
    fun sendMultipartMessage(
            roomId: String,
            parts: List<NewPart>,
            callback: (Result<Int, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.sendMultipartMessage(roomId, parts) }, callback)

    /**
     * Sets that the current user is typing in the [room]. You can send as many of these as you like,
     * the SDK will handle rate limiting the request. The [callback] will be called with a [Result]
     * when the operation is complete. If the operation was successful you will receive a [Unit], or
     * an [Error] informing you of what went wrong.
     */
    fun isTypingIn(room: Room, callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.isTypingIn(room) }, callback)

    /**
     * @see [isTypingIn]
     */
    fun isTypingIn(roomId: String, callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.isTypingIn(roomId) }, callback)

    /**
     * Get a list of the public rooms that you are not a member of yet. The [callback] will be called
     * with a [Result] when the operation is complete. If the operation was successful you will receive
     * a List<Room>, or an [Error] informing you of what went wrong.
     */
    fun getJoinableRooms(callback: (Result<List<Room>, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.getJoinableRooms() }, callback)

    /**
     * Get the list of users in the [room]. The [callback] will be called with a [Result] when the
     * operation is complete. If the operation was successful you will receive a List<User>, or an
     * [Error] informing you of what went wrong.
     */
    fun usersForRoom(room: Room, callback: (Result<List<User>, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.usersForRoom(room) }, callback)

    /**
     * @see [usersForRoom]
     */
    fun usersForRoom(roomId: String, callback: (Result<List<User>, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.usersForRoom(roomId) }, callback)

    /**
     * Enables push notifications for the current user. The [callback] will be called with a [Result]
     * when the operation is complete. If the operation was successful you will receive a [Unit],
     * or an [Error] informing you of what went wrong.
     */
    fun enablePushNotifications(callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.enablePushNotifications() }, callback)
}
