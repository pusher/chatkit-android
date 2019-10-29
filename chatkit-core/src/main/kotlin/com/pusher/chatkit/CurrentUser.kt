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
     * Fetches a list of all the users in rooms you are currently subscribed to.
     * The [callback] will be called with a [Result] when the
     * operation is complete. If the operation was successful you will receive a [List<User>] which
     * contains all the users, or an [Error] informing you of what went wrong.
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

    @Deprecated("Not intended for to be used outside of internal implementation ")
    fun updateWithPropertiesOf(newUser: CurrentUser) =
            syncCurrentUser.updateWithPropertiesOf(newUser.syncCurrentUser)

    /**
     * Sets the read cursor for the current user in [room] to [position].
     * The [position] is the id of the last message that the current user has read.
     * Setting your read cursor will update your unread count for this room for the current user.
     * This will additionally notify any other connected users who have a [RoomListeners]
     * set for [onNewReadCursor] to let them know this current user has read up to [position].
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
     * Gets the current users read cursor for the [roomId]. This method is synchronous and you will
     * receive a [Result] with a [Cursor] if the process was successful, or an [Error] informing you
     * of what went wrong. A common error is you must first be subscribed to the room to see read cursors.
     */
    fun getReadCursor(roomId: String): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(roomId)
    /**
     * @see [getReadCursor]
     */
    fun getReadCursor(room: Room): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(room.id)

    /**
     * Gets the current read cursor for the [userId] in [roomId]. This method is synchronous and you will
     * receive a [Result] with a [Cursor] if the process was successful, or an [Error] informing you
     * of what went wrong. A common error is you must first be subscribed to the room to see read cursors.
     */
    fun getReadCursor(roomId: String, userId: String): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(roomId, userId)

    /**
     * @see [getReadCursor]
     */
    fun getReadCursor(room: Room, user: User): Result<Cursor, Error> =
            syncCurrentUser.getReadCursor(room.id, user.id)

    /**
     * Make the users listed in [userIds] members of the room identified by [roomId]. This does not mean
     * the user(s) will see new messages immediately, it means the room will be in their list of rooms
     * and they will need to subscribe themselves to see any updates.
     * Calling this method requires the current user to have the room:members:add permission.
     * The [callback] will be called with a [Result] when the
     * operation is complete. If the operation was successful there is no value returned, or an
     * [Error] informing you of what went wrong.
     */
    fun addUsersToRoom(roomId: String, userIds: List<String>, callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.addUsersToRoom(roomId, userIds) }, callback)

    /**
     * Removes the [userIds] from the room identified by [roomId].
     * Calling this method requires the current user to have the room:members:remove permission.
     * The [callback] will be called with a [Result] when the
     * operation is complete. If the operation was successful there is no value returned, or an
     * [Error] informing you of what went wrong.
     */
    fun removeUsersFromRoom(
            roomId: String,
            userIds: List<String>,
            callback: (Result<Unit, Error>) -> Unit
    ) = makeCallback({ syncCurrentUser.removeUsersFromRoom(roomId, userIds) }, callback)

    /**
     * Create a new room with:
     *
     * @param[id] optional - a uniquely identifying String, if not provided a randomly generated unique string will be created for you
     * @param[name] a String to label the room
     * @param[pushNotificationTitleOverride] optional - a string that appears on the push notification
     * title for this room; by default the title is the room name, or for rooms that just have two members the title
     * will be the other users name.
     * @param[isPrivate] optional - a boolean to determine if the room is available for others to join, by default the room will be not be private
     * @param[customData] optional - a [CustomData] object with any extra information you'd like to save with the new room e.g. mapOf("description" to "some extra description about the room")
     * @param[userIds] optional - a list of the userIds of users who will be added to the room
     * @param[callback] returns a [Result] with the [Room] if successful, or an [Error] letting you know what went wrong

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
     * Update an existing [room]'s properties. If an argument is omitted or provided as null,
     * the existing value will not be changed.
     * - [name] - a String to label the room
     * - [pushNotificationTitleOverride] - optional - a string that appears on the push notification title for this room, by default the title is the room name
     * - [isPrivate]- optional - a boolean to determine if the room is available for others to join, by default the room will be not be private
     * - [customData] - optional - a [CustomData] object with any extra information you'd like to save with the new room e.g. mapOf("description" to "some extra description about the room")
     * - [callback] - returns a [Result] with the [Room] if successful, or an [Error] letting you know what went wrong
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
     * If the operation was successful you will receive a [String] which contains the deleted room id,
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

    /**
     * Remove yourself from the [room]. The [callback] will be called with a [Result] when the operation is complete.
     * If the operation was successful you will receive a [String] which contains the room id of the room you just left,
     * or an [Error] informing you of what went wrong.
     */
    fun leaveRoom(room: Room, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.leaveRoom(room) }, callback)

    /**
     * @see [leaveRoom]
     */
    fun leaveRoom(roomId: String, callback: (Result<String, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.leaveRoom(roomId) }, callback)

    /**
     * Add yourself to the [room]. The [callback] will be called with a [Result] when the operation is complete.
     * If the operation was successful you will receive a [Room] which contains room you just joined,
     * or an [Error] informing you of what went wrong.
     */
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
     * Subscribe to a [room] for multipart messages. You need to configure the [listeners] to listen
     * for the events you're interested in, for more information on what events are possible see the [docs][https://pusher.com/docs/chatkit/reference/android#chat-events]
     * You can configure a [messageLimit] which returns the 10 messages that were most recently sent to the room,
     * each message will call the [onMultipartMessage] listener so you can determine how to display the message.
     * Finally the [callback] will be called with a [Subscription] if the operation was successful.
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
     * Fetches multipart messages for a [roomId] from an optional message [initialId].
     * @param[roomId] the room id you want to get messages for
     * @param[initialId] optional - the message id for the [0] message, if no initialId is provided the [0] message is the last message sent to the room
     * @param[direction] optional - the direction messages are ordered in the list, by default this is [Direction.OLDER_FIRST], alternatively you can request the messages in [Direction.NEWER_FIRST]
     * @param[limit] optional - the number of messages you want to receive, by default this will be 10 messages.
     * @param[callback] returns a [Result] with the [List<com.pusher.chatkit.messages.multipart.Message>] if successful, or an [Error] letting you know what went wrong
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
     * operation was successful you will receive an [Int] which contains the sent message id, or an
     * [Error] informing you of what went wrong.
     *
     * e.g.
     * `sendMultipartMessage(room, listOf(`
     * `NewPart.Inline("hello world"),`
     * `NewPart.Url("https://example.com/uploads/myphoto.png", "image/png") ), callback = { })`
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
     * the SDK will handle rate limiting the requests. The [callback] will be called with a [Result]
     * when the operation is complete. If the operation was successful there is no value returned, or
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
     * Get a list of the public rooms that you are not a member of yet.
     * The memberUserIds, unreadCount, and lastMessageId will not be populated until you join and subscribe to the room.
     * The [callback] will be called with a [Result] when the operation is complete.
     * If the operation was successful you will receive a List<Room>, or an [Error] informing you of what went wrong.
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
     * Enables push notifications for the current user.
     *
     * You can call this every time a user connects successfully to Chatkit,
     * a user can receive push notifications on up to 10 Android devices. However you should disable
     * push notifications if you detect the user in a signed out state to ensure they don't accidentally receive further
     * push notifications. To disable push notifications for the current user call `chatManager.disablePushNotifications {  }`
     * More information on this is available in our
     * [docs][https://pusher.com/docs/chatkit/guides/push_notifications/setup-android#disabling-push-notifications])
     *
     * The [callback] will be called with a [Result] when the operation is complete.
     * If the operation was successful there is no value returned, or an [Error] informing you of what went wrong.
     */
    fun enablePushNotifications(callback: (Result<Unit, Error>) -> Unit) =
            makeCallback({ syncCurrentUser.enablePushNotifications() }, callback)
}
