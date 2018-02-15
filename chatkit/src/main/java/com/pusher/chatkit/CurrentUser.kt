package com.pusher.chatkit

import android.os.Handler
import android.os.Looper
import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.ChatManager.Companion.GSON
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import com.google.gson.reflect.TypeToken
import com.pusher.platform.RequestDestination
import elements.Error
import kotlinx.coroutines.experimental.async
import java.io.File
import kotlin.coroutines.experimental.suspendCoroutine


class CurrentUser(
        rooms: List<Room>,
        val apiInstance: Instance,
        val createdAt: String,
        val cursors: ConcurrentHashMap<Int, Cursor>,
        val cursorsInstance: Instance,
        val id: String,
        val logger: Logger,
        val filesInstance: Instance,
        val tokenParams: ChatkitTokenParams?,
        val tokenProvider: TokenProvider,
        val userStore: GlobalUserStore,
        var avatarURL: String?,
        var customData: CustomData?,
        var name: String?,
        var updatedAt: String
) {
    val mainThread = Handler(Looper.getMainLooper())

    fun updateWithPropertiesOf(newUser: User){
        updatedAt = newUser.updatedAt
        name = newUser.name
        customData = newUser.customData
    }

    var presenceSubscription: PresenceSubscription? = null
    val roomStore: RoomStore

    init {
        val roomMap = ConcurrentHashMap<Int, Room>()
        rooms.forEach { room ->
            roomMap.put(room.id, room)
        }
        roomStore = RoomStore(instance = apiInstance, rooms = roomMap)
    }

    fun rooms(): Set<Room> = roomStore.setOfRooms()
    fun getRoom(id: Int): Room? = roomStore.rooms[id]

    //Room membership related information
    @JvmOverloads fun createRoom(
            name: String,
            isPrivate: Boolean = false,
            userIds: Array<String>? = null,
            onRoomCreatedListener: RoomListener,
            onErrorListener: ErrorListener
    ){
        val roomRequest = RoomCreateRequest(
                name = name,
                private = isPrivate,
                createdById = id,
                userIds = userIds
        )

        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = "/rooms",
                        body = GSON.toJson(roomRequest)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->
                    val room = GSON.fromJson<Room>(response.body()!!.charStream(), Room::class.java)

                    roomStore.addOrMerge(room)
                    populateRoomUserStore(room)

                    mainThread.post { onRoomCreatedListener.onRoom(room) }

                },
                onFailure = { error ->
                    mainThread.post { onErrorListener.onError(error) }
                }
        )
    }

    @JvmOverloads fun getUserRooms(onlyJoinable: Boolean = false, onCompleteListener: RoomsListener){

        val roomListType = object : TypeToken<List<Room>>() {}.getType()
        val path = "/users/$id/rooms"
        apiInstance.request(
                options = RequestOptions(
                    method = "GET",
                    path = path+"?joinable=$onlyJoinable"
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->
                    val rooms = GSON.fromJson<List<Room>>(response.body()!!.string(), roomListType)
                    onCompleteListener.onRooms(rooms)
                },
                onFailure = {
                    logger.error("Tragedy! No rooms could have been returned!")
                }
        )
    }

    fun getJoinableRooms(onCompleteListener: RoomsListener){
        getUserRooms(onlyJoinable = true, onCompleteListener = onCompleteListener)
    }

    @JvmOverloads fun subscribeToRoom(
            room: Room,
            messageLimit: Int = 20,
            listeners: RoomSubscriptionListeners,
            cursorsListeners: CursorsSubscriptionListeners? = null
    ){
        val roomSubscription = RoomSubscription(this, room, userStore, listeners)
        apiInstance.subscribeResuming(
                path = "/rooms/${room.id}?user_id=$id&message_limit=$messageLimit",
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = roomSubscription.subscriptionListeners
        )
        if (cursorsListeners == null) {
            return
        }
        val cursorsSubscription = CursorsSubscription(this, room, userStore, cursorsListeners)
        cursorsInstance.subscribeResuming(
                path = "/cursors/0/rooms/${room.id}/",
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                listeners = cursorsSubscription.subscriptionListeners
        )
    }

    fun setCursor(
            position: Int,
            room: Room,
            onCompleteListener: SetCursorListener,
            onErrorListener: ErrorListener
    ) {
        cursorsInstance.request(
                options = RequestOptions(
                        method = "PUT",
                        path = "/cursors/0/rooms/${room.id}/users/$id",
                        body = GSON.toJson(SetCursorRequest(position))
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { onCompleteListener.onSetCursor() },
                onFailure = { onErrorListener.onError(it) }
        )
    }

    private fun populateRoomUserStore(room: Room) {

        room.memberUserIds.forEach { userId ->

            userStore.findOrGetUser(
                    id = userId,
                    userListener = UserListener { user -> room.userStore().addOrMerge(user) },
                    errorListener = ErrorListener {
                        TODO("Not implemented")

                    }
            )
        }
    }

    @JvmOverloads
    fun sendMessage(
            roomId: Int,
            text: String? = null,
            attachment: GenericAttachment = NoAttachment,
            onCompleteListener: MessageSentListener,
            onErrorListener: ErrorListener
    ) {
        async {
            val attachmentBody = when (attachment) {
                is DataAttachment -> {
                    val uploadRes = uploadFile(
                        file = attachment.file,
                        fileName = attachment.name,
                        roomId = roomId
                    )

                    when (uploadRes) {
                        is Ok -> uploadRes.value
                        is Err -> AttachmentBody.Failed(uploadRes.error)
                    }
                }
                is LinkAttachment -> AttachmentBody.Resource(attachment.link, attachment.type)
                is NoAttachment -> null
            }

            if (attachmentBody is AttachmentBody.Failed) {
                logger.error("Failed to upload file: ${attachmentBody.error}")
                onErrorListener.onError(attachmentBody.error)
            } else {
                val sendMessageRes = sendCompleteMessage(
                    roomId = roomId,
                    text = text,
                    attachment = attachmentBody
                )

                when (sendMessageRes) {
                    is Ok -> onCompleteListener.onMessage(sendMessageRes.value.messageId)
                    is Err -> onErrorListener.onError(sendMessageRes.error)
                }
            }
        }
    }

    private suspend fun sendCompleteMessage(
            roomId: Int,
            text: String? = null,
            attachment: AttachmentBody? = null
    ): Result<MessageSendingResponse, Error> = suspendCoroutine {
        val path = "/rooms/$roomId/messages"
        val message = MessageRequest(text = text, userId = id, attachment = attachment)
        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = path,
                        body = GSON.toJson(message)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { res ->
                    val messageRes = GSON.fromJson<MessageSendingResponse>(res.body()!!.charStream(), MessageSendingResponse::class.java)
                    it.resume(Ok(messageRes))
                },
                onFailure = { err -> it.resume(Err(err)) }
        )
    }

    fun fetchAttachment(
            attachmentUrl: String,
            onCompleteListener: FetchedAttachmentListener,
            onErrorListener: ErrorListener
    ) {
        filesInstance.request(
                options = RequestOptions(
                        method = "GET",
                        destination = RequestDestination.Absolute(attachmentUrl)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->
                    val fetchedAttachment = GSON.fromJson<FetchedAttachment>(response.body()!!.charStream(), FetchedAttachment::class.java)
                    onCompleteListener.onFetch(fetchedAttachment)
                },
                onFailure = { error -> onErrorListener.onError(error) }

        )
    }

    private suspend fun uploadFile(
            file: File,
            fileName: String,
            roomId: Int
    ): Result<AttachmentBody, Error> = suspendCoroutine {
        filesInstance.upload(
                path = "/rooms/$roomId/files/$fileName",
                file = file,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { res ->
                    val attachmentBody = GSON.fromJson<AttachmentBody>(res.body()!!.charStream(), AttachmentBody::class.java)
                    it.resume(Ok(attachmentBody))
                },
                onFailure = { err -> it.resume(Err(err)) }
        )
    }

    fun addUsers(roomId: Int, users: Array<User>, completeListener: OnCompleteListener, errorListener: ErrorListener) = addUsers(roomId, users.map { id }.toTypedArray(), completeListener, errorListener)
    fun addUsers(roomId: Int, userIds: Array<String>, completeListener: OnCompleteListener, errorListener: ErrorListener) = addOrRemoveUsers("add", roomId, userIds, completeListener, errorListener)

    fun removeUsers(roomId: Int, users: Array<User>, completeListener: OnCompleteListener, errorListener: ErrorListener) = removeUsers(roomId, users.map { id }.toTypedArray(), completeListener, errorListener)
    fun removeUsers(roomId: Int, userIds: Array<String>, completeListener: OnCompleteListener, errorListener: ErrorListener) = addOrRemoveUsers("remove", roomId, userIds, completeListener, errorListener)

    private fun addOrRemoveUsers(
            operation: String,
            roomId: Int,
            userIds: Array<String>,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener){

        val data = object {
            val userIds = userIds
        }

        val path = "/rooms/$roomId/users/$operation"
        apiInstance.request(
                options = RequestOptions(
                        method = "PUT",
                        path = path,
                        body = GSON.toJson(data)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { completeListener.onComplete() },
                onFailure = { error -> errorListener.onError(error) }
        )
    }

    /**
     * Update a room
     * */

    fun updateRoom(
            room: Room,
            name: String? = null,
            isPrivate: Boolean? = null,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener
    ){
        val path = "/rooms/${room.id}"
        val data = UpdateRoomRequest(
                    name = name ?: room.name,
                    isPrivate =  isPrivate ?: room.isPrivate
        )

        apiInstance.request(
                options = RequestOptions(
                        method = "PUT",
                        path = path,
                        body = GSON.toJson(data)
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { mainThread.post {  completeListener.onComplete() } },
                onFailure = { error -> mainThread.post { errorListener.onError(error) } }
        )
    }

    data class UpdateRoomRequest(val name: String, val isPrivate: Boolean)

    /**
     * Delete a room
     * */

    fun deleteRoom(room: Room, completeListener: OnCompleteListener, errorListener: ErrorListener) = deleteRoom(room.id, completeListener, errorListener)

    fun deleteRoom(
            roomId: Int,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener
    ){
        val path = "/rooms/$roomId"
        apiInstance.request(
                options = RequestOptions(
                        method = "DELETE",
                        path = path,
                        body = ""
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { mainThread.post {  completeListener.onComplete() } },
                onFailure = { error -> mainThread.post { errorListener.onError(error) } }
        )
    }

    /**
     * Join a room
     * */
    fun joinRoom(room: Room, completeListener: RoomListener, errorListener: ErrorListener) = joinRoom(room.id, completeListener, errorListener)

    fun joinRoom(
            roomId: Int,
            completeListener: RoomListener,
            errorListener: ErrorListener
    ){
        val wrappedCompleteListener = RoomListener { room -> mainThread.post { completeListener.onRoom(room) }}
        val wrappedErrorListener = ErrorListener { error -> mainThread.post { errorListener.onError(error) }}

        val path = HttpUrl.parse("https://pusherplatform.io")!!.newBuilder().addPathSegments("/users/$id/rooms/$roomId/join").build().encodedPath()

        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = path,
                        body = "" //TODO: this is a horrible OKHTTP hack - POST is required to have a body.
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->

                    val room = GSON.fromJson<Room>(response.body()!!.charStream(), Room::class.java)
                    roomStore.addOrMerge(room)
                    populateRoomUserStore(room)
                    wrappedCompleteListener.onRoom(room)

                },
                onFailure = { error -> wrappedErrorListener.onError(error) }
        )
    }


    /**
     * Leave a room
     * */
    fun leaveRoom(room: Room, completeListener: OnCompleteListener, errorListener: ErrorListener) = leaveRoom(room.id, completeListener, errorListener)

    fun leaveRoom(
            roomId: Int,
            completeListener: OnCompleteListener,
            errorListener: ErrorListener
    ){
        val path = HttpUrl.parse("https://pusherplatform.io")!!.newBuilder().addPathSegments("/users/$id/rooms/$roomId/leave").build().encodedPath()

        apiInstance.request(
                options = RequestOptions(
                        method = "POST",
                        path = path,
                        body = "" //TODO: this is a horrible OKHTTP hack - POST is required to have a body.
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { completeListener.onComplete() },
                onFailure = { error -> errorListener.onError(error) }
        )
    }

    fun establishPresenceSubscription(listeners: ThreadedUserSubscriptionListeners) {

        presenceSubscription = PresenceSubscription(
                instance = apiInstance,
                path = "/users/$id/presence",
                listeners = listeners,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                userStore = userStore,
                logger = logger
        )
    }
}

data class MessageRequest(val text: String? = null, val userId: String, val attachment: AttachmentBody? = null)

sealed class AttachmentBody {
    data class Resource(val resourceLink: String, val type: String) : AttachmentBody()
    data class Failed(val error: Error) : AttachmentBody()
}

data class SetCursorRequest(val position: Int)

data class MessageSendingResponse(val messageId: Int)

data class RoomCreateRequest(
        val name: String,
        val private: Boolean,
        val createdById: String,
        var userIds: Array<String>? = null
)

data class FetchedAttachment(
        val file: FetchedAttachmentFile,
        @SerializedName("resource_link") val link: String,
        val ttl: Double
)

data class FetchedAttachmentFile(val bytes: Int, val lastModified: Double, val name: String)
