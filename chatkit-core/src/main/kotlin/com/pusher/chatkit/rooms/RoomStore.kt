package com.pusher.chatkit.rooms

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
                // TODO: extract well named funs with clear responsibility
                is UserSubscriptionEvent.InitialState -> {
                    val knownRooms = this.toList()

                    val addedTo = event.rooms.filterNot {
                        knownRooms.contains(it)
                    }.onEach {
                        this += it
                    }.map { room ->
                        UserSubscriptionEvent.AddedToRoomEvent(room)
                    }

                    val removedFrom = knownRooms.filterNot {
                        event.rooms.contains(it)
                    }.onEach {
                        this -= it
                    }.map {
                        UserSubscriptionEvent.RemovedFromRoomEvent(it.id)
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

                    val usersJoined = knownRooms.filterPairingWith(event.rooms).filter { (kr, nr) ->
                        !kr.memberUserIds.containsAll(nr.memberUserIds)
                    }.onEach { (_, nr) ->
                        this += nr
                    }.map { (kr, nr) ->
                        Pair(nr.id, nr.memberUserIds - kr.memberUserIds)
                    }.flatMap { (roomId, joinedMembers) ->
                        joinedMembers.map { joinedMember ->
                            UserSubscriptionEvent.UserJoinedRoomEvent(joinedMember, roomId)
                        }
                    }

                    val usersLeft = knownRooms.filterPairingWith(event.rooms).filter { (kr, nr) ->
                        !nr.memberUserIds.containsAll(kr.memberUserIds)
                    }.onEach { (_, nr) ->
                        this += nr
                    }.map { (kr, nr) ->
                        Pair(nr.id, kr.memberUserIds - nr.memberUserIds)
                    }.flatMap { (roomId, leftMembers) ->
                        leftMembers.map { leftMember ->
                            UserSubscriptionEvent.UserLeftRoomEvent(leftMember, roomId)
                        }
                    }

                    listOf(event) + addedTo + removedFrom + updated + usersJoined + usersLeft
                }
                is UserSubscriptionEvent.AddedToRoomApiEvent ->
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
                is UserSubscriptionEvent.UserJoinedRoomEvent -> {
                    val room = roomsMap[event.roomId]!!
                    this += room.withAddedMember(event.userId)
                    listOf(event)
                }
                is UserSubscriptionEvent.UserLeftRoomEvent -> {
                    val room = roomsMap[event.roomId]!!
                    this += room.withLeftMember(event.userId)
                    listOf(event)
                }
                else -> listOf(event)
            }

    private fun List<Room>.filterPairingWith(rooms: List<Room>): List<Pair<Room, Room>> {
        return this.map { kr ->
            Pair(kr, rooms.find { nr -> kr.id == nr.id })
        }.filter { (_, nr) ->
            nr != null
        }.map { (kr, nr) ->
            Pair(kr, nr!!)
        }
    }

}