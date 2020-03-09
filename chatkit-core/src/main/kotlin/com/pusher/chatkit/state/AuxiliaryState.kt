package com.pusher.chatkit.state

internal data class LastChange<T>(val version: Int, val action: Action)

@Suppress("CopyWithoutNamedArguments") // nextVersion (legible) passes as first param many times
internal data class AuxiliaryState(
    val version: Int,
    val joinedRoomsReceived: LastChange<JoinedRoomsReceived>?,
    val joinedRoom: LastChange<JoinedRoom>?,
    val leftRoom: LastChange<LeftRoom>?,
    val roomUpdated: LastChange<RoomUpdated>?,
    val roomDeleted: LastChange<RoomDeleted>?,
    val reconnectJoinedRoom: LastChange<ReconnectJoinedRoom>?,
    val unreadCountReceived: LastChange<UnreadCountReceived>?
) {

    companion object {
        fun initial() = AuxiliaryState(
            version = 0,
            joinedRoomsReceived = null,
            joinedRoom = null,
            leftRoom = null,
            roomUpdated = null,
            roomDeleted = null,
            reconnectJoinedRoom = null,
            unreadCountReceived = null
        )
    }

    fun with(action: Action) =
        when (action) {
            is JoinedRoomsReceived ->
                copy(nextVersion, joinedRoomsReceived = LastChange(nextVersion, action))
            is JoinedRoom ->
                copy(nextVersion, joinedRoom = LastChange(nextVersion, action))
            is LeftRoom ->
                copy(nextVersion, leftRoom = LastChange(nextVersion, action))
            is RoomDeleted ->
                copy(nextVersion, roomDeleted = LastChange(nextVersion, action))
            is RoomUpdated ->
                copy(nextVersion, roomUpdated = LastChange(nextVersion, action))
            is ReconnectJoinedRoom ->
                copy(nextVersion, reconnectJoinedRoom = LastChange(nextVersion, action))
            is UnreadCountReceived ->
                copy(nextVersion, unreadCountReceived = LastChange(nextVersion, action))
        }

    private val nextVersion = version + 1
}
