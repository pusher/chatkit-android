package com.pusher.chatkit.users

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.Room


internal typealias UserSubscriptionConsumer = (UserSubscriptionEvent) -> Unit

internal sealed class UserSubscriptionEvent {

    internal data class InitialState(
            val currentUser: User,
            @SerializedName("rooms") private var _rooms: List<Room>,
            val readStates: List<ReadStateApiType>,
            val memberships: List<RoomMembershipApiType> // TODO: make priv and relay via room
    ) : UserSubscriptionEvent() {

        val rooms: List<Room>
            get() {
                if (!populatedRoomUnreadCounts) {
                    val readStateRoomId2PositionMap : MutableMap<String, Int> =
                            readStates.mapIndexed { index, readState -> Pair(readState, index) }
                                    .associateTo(HashMap(readStates.size)) { (readState, index) ->
                                        Pair(readState.roomId, index)
                                    }

                    _rooms = _rooms.map { room ->
                        val readStatePosition : Int? = readStateRoomId2PositionMap.remove(room.id)
                        val readState = if (readStatePosition != null) {
                            readStates[readStatePosition]
                        } else {
                            null
                        }
                        val memberIds = memberships.find { it.roomId == room.id }!!.userIds.toSet() // TODO: perf
                        Triple(room, readState, memberIds)
                    }.map { (room, readState, memberIds) ->
                        if (readState != null) {
                            room.copy(unreadCount = readState.unreadCount,
                                    memberUserIds = memberIds)
                        } else {
                            room
                        }
                    }
                    populatedRoomUnreadCounts = true
                }
                return _rooms
            }
        private var populatedRoomUnreadCounts = false

        val cursors: List<Cursor>
            get() = readStates.filter { it.cursor != null }
                    .map { it.cursor!! }

    }

    internal data class AddedToRoomEvent(
            @SerializedName("room") private var _room: Room,
            val readState: ReadStateApiType,
            val memberships: RoomMembershipApiType // TODO: make priv and relay via room
    ) : UserSubscriptionEvent() {

        val room : Room
            get() = _room.copy(unreadCount = readState.unreadCount,
                    memberUserIds = memberships.userIds.toSet())

    }

    internal data class RemovedFromRoomEvent(val roomId: String) : UserSubscriptionEvent()

    internal data class RoomUpdatedEvent(val room: Room) : UserSubscriptionEvent()

    internal data class RoomDeletedEvent(val roomId: String) : UserSubscriptionEvent()

    internal data class ReadStateUpdatedEvent(
            val readState: ReadStateApiType) : UserSubscriptionEvent()

    internal data class UserJoinedRoomEvent(
            val userId: String,
            val roomId: String) : UserSubscriptionEvent()

    internal data class UserLeftRoomEvent(
            val userId: String,
            val roomId: String) : UserSubscriptionEvent()

    internal data class ErrorOccurred(val error: elements.Error) : UserSubscriptionEvent()
}

internal data class ReadStateApiType(
        val roomId: String,
        val unreadCount: Int,
        val cursor: Cursor?
)

internal data class RoomMembershipApiType(val roomId: String, val userIds: List<String>)