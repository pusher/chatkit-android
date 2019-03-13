package com.pusher.chatkit.rooms

import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class RoomStore(
        private val roomsMap: MutableMap<String, Room> = ConcurrentHashMap()
) {
    private var initialized = AtomicBoolean(false)

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
        initialized.set(false)
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
                        this += it
                    }.map {
                        UserSubscriptionEvent.RoomUpdatedEvent(it)
                    }

                    if (initialized.getAndSet(true)) {
                        listOf(event) + removedFrom + addedTo + updated
                    } else {
                        listOf(event)
                    }
                }
                is UserSubscriptionEvent.AddedToRoomEvent ->
                    listOf(event.also { this += event.room })
                is UserSubscriptionEvent.RoomUpdatedEvent ->
                    listOf(event.also { this += event.room })
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
