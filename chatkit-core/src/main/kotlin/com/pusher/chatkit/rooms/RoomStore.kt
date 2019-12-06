package com.pusher.chatkit.rooms

import com.pusher.chatkit.memberships.MembershipSubscriptionEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import java.util.concurrent.ConcurrentHashMap

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
                    }.map { room ->
                        UserSubscriptionEvent.AddedToRoomEvent(room,
                                event.readStates.find { it.roomId == room.id }!!,
                                event.memberships.find { it.roomId == room.id }!!)
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

                    listOf(event) + removedFrom + addedTo + updated
                }
                is UserSubscriptionEvent.AddedToRoomEvent ->
                    listOf(event.also {
                        this += event.room
                    })
                is UserSubscriptionEvent.RoomUpdatedEvent -> {
                    var newRoom = event.room
                    val knownRoom = roomsMap[newRoom.id]!!
                    // we don't get unread count and members with this event
                    newRoom = newRoom.copy(memberUserIds = knownRoom.memberUserIds)
                    if (knownRoom.unreadCount != null) {
                        // we only get unread counts in initial state for 1000 rooms
                        newRoom = newRoom.copy(unreadCount = knownRoom.unreadCount)
                    }
                    this += newRoom
                    listOf(UserSubscriptionEvent.RoomUpdatedEvent(newRoom))
                }
                is UserSubscriptionEvent.ReadStateUpdatedEvent -> {
                    var room = roomsMap[event.readState.roomId]!!
                    room = room.copy(unreadCount = event.readState.unreadCount)
                    this += room

                    // Returning a copy with no cursor so that the returned applied event is
                    // not translated to NewReadCursor but to RoomUpdated ChatEvent.
                    // That is the relevant event from the perspective of this store.
                    // The ChatEvent.NewReadCursor will be translated
                    // into if CursorStore updates applying ReadStateUpdatedEvent and
                    // returning it with the cursor.
                    val eventWithNoCursor = event.copy(event.readState.copy(cursor = null))
                    listOf(eventWithNoCursor)
                }
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
                        // TODO: now that comes within user's subscription
//                        is MembershipSubscriptionEvent.UserJoined -> this[roomId]?.addUser(event.userId)
//                        is MembershipSubscriptionEvent.UserLeft -> this[roomId]?.removeUser(event.userId)
                    }
                }
            }
}
