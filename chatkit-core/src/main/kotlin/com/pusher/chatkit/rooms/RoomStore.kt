package com.pusher.chatkit.rooms

import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class RoomStore(
        private val roomsMap: MutableMap<String, Room> = ConcurrentHashMap()
) {
    fun toList(): List<Room> =
            roomsMap.values.toList()

    operator fun get(id: String): Room? =
            roomsMap[id]

    operator fun plusAssign(room: Room) {
        roomsMap += (room.id to room)
    }

    operator fun plusAssign(rooms: List<Room>) {
        roomsMap += rooms.map { it.id to it }.toMap()
    }

    operator fun minusAssign(room: Room) {
        roomsMap -= room.id
    }

    operator fun minusAssign(rooms: List<Room>) {
        roomsMap -= rooms.map { it.id }
    }

    operator fun minusAssign(roomId: String) {
        roomsMap -= roomId
    }

    fun clear() {
        roomsMap.clear()
    }

    fun initialiseContents(rooms: List<Room>) {
        clear()
        rooms.forEach { this += it }
    }

    fun applyUserSubscriptionEvent(
            event: UserSubscriptionEvent
    ): List<UserSubscriptionEvent> =
            when (event) {
                is UserSubscriptionEvent.InitialState -> {
                    val knownRooms = this.toList()
                    val removedFrom = knownRooms.filterNot {
                        event.rooms.contains(it)
                    }.onEach {
                        this -= it
                    }.map {
                        UserSubscriptionEvent.RemovedFromRoomEvent(it.id)
                    }

                    val addedTo = event.rooms.filterNot {
                        knownRooms.contains(it)
                    }.onEach {
                        this += it
                    }.map {
                        UserSubscriptionEvent.AddedToRoomEvent(it)
                    }

                    val updated = event.rooms.filter { nr ->
                        knownRooms.any { kr ->
                            kr == nr && !kr.deepEquals(nr)
                        }
                    }.onEach {
                        it.addAllUsers(roomsMap[it.id]?.memberUserIds.orEmpty())
                        this += it
                    }.map {
                        UserSubscriptionEvent.RoomUpdatedEvent(it)
                    }

                    listOf(event) + removedFrom + addedTo + updated
                }
                is UserSubscriptionEvent.AddedToRoomEvent ->
                    listOf(event.also {
                        event.room.addAllUsers(roomsMap[event.room.id]?.memberUserIds.orEmpty())
                        this += event.room
                    })
                is UserSubscriptionEvent.RoomUpdatedEvent ->
                    listOf(event.also {
                        //memberUserIDs are not populated in Rooms we have just deserialised from the
                        // server because we receive them separately via membership subscriptions,
                        // so we must copy the set we have been tracking on our previous instance
                        // of the Room on to this new instance
                        event.room.addAllUsers(roomsMap[event.room.id]?.memberUserIds.orEmpty())
                        this += event.room})
                is UserSubscriptionEvent.RoomDeletedEvent ->
                    listOf(event.also { this -= event.roomId })
                is UserSubscriptionEvent.RemovedFromRoomEvent ->
                    listOf(event.also { this -= event.roomId })
                else -> listOf(event)
            }

    fun applyMembershipEvent(
            roomId: String,
            event: MembershipSubscriptionEvent
    ): List<MembershipSubscriptionEvent> =
            when (event) {
                is MembershipSubscriptionEvent.InitialState -> {
                    val existingMembers = this[roomId]?.memberUserIds.orEmpty()

                    val joinedIds = event.userIds.filterNot { existingMembers.contains(it) }
                    val leftIds = existingMembers.filterNot { event.userIds.contains(it) }

                    joinedIds.map(MembershipSubscriptionEvent::UserJoined) +
                            leftIds.map(MembershipSubscriptionEvent::UserLeft)
                }
                else ->
                    listOf(event)
            }.also { events ->
                events.forEach { event ->
                    when (event) {
                        is MembershipSubscriptionEvent.UserJoined -> this[roomId]?.addUser(event.userId)
                        is MembershipSubscriptionEvent.UserLeft -> this[roomId]?.removeUser(event.userId)
                    }
                }
            }
}
