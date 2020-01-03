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
                    val oldRooms = this.toList()
                    val newRooms = event.rooms

                    val pairs = oldRooms.zipOn(newRooms) { it.id }

                    val addedTo = pairs.takeSecondWhereFirstIsNull().onEach {
                        this += it
                    }.map { room ->
                        UserSubscriptionEvent.AddedToRoomEvent(
                                room,
                                event.readStates.find { it.roomId == room.id }!!,
                                event.memberships.find { it.roomId == room.id }!!
                        )
                    }

                    val removedFrom = pairs.takeFirstWhereSecondIsNull().onEach {
                        this -= it
                    }.map {
                        UserSubscriptionEvent.RemovedFromRoomEvent(it.id)
                    }

                    val matchingPairs = pairs.takeWhereNeitherAreNull()

                    val updated = matchingPairs.filter {
                        !it.first.deepEquals(it.second)
                    }.map {
                        it.second
                    }.onEach {
                        this += it
                    }.map {
                        UserSubscriptionEvent.RoomUpdatedEvent(it)
                    }

                    val usersJoined = matchingPairs.map {
                        it.second to (it.first.memberUserIds - it.second.memberUserIds)
                    }.filter { (_, newMembers) ->
                        newMembers.isNotEmpty()
                    }.onEach { (newRoom, _) ->
                        this += newRoom
                    }.flatMap { (newRoom, newMembers) ->
                        newMembers.map { newMember ->
                            UserSubscriptionEvent.UserJoinedRoomEvent(newMember, newRoom.id)
                        }
                    }

                    val usersLeft = matchingPairs.map {
                        it.second to (it.second.memberUserIds - it.first.memberUserIds)
                    }.filter { (_, oldMembers) ->
                        oldMembers.isNotEmpty()
                    }.onEach { (newRoom, _) ->
                        this += newRoom
                    }.flatMap { (newRoom, oldMembers) ->
                        oldMembers.map { oldMember ->
                            UserSubscriptionEvent.UserJoinedRoomEvent(oldMember, newRoom.id)
                        }
                    }

                    listOf(event) + addedTo + removedFrom + updated + usersJoined + usersLeft
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

    private fun <V, K: Comparable<K>> List<V>.zipOn(others: List<V>, on: (V) -> K): List<Pair<V?, V?>> =
            (this.map { on(it) } + others.map { on(it) })
                    .toSet()
                    .sorted()
                    .map { id ->
                        this.find { on(it) == id } to others.find { on(it) == id }
                    }

    private fun <A, B> List<Pair<A?, B?>>.takeWhereNeitherAreNull(): List<Pair<A, B>> =
            this.mapNotNull {
                if (it.first != null && it.second != null)
                    it.first!! to it.second!!
                else
                    null
            }

    private fun <A, B> List<Pair<A?, B?>>.takeFirstWhereSecondIsNull(): List<A> =
            this.filter { it.second == null }.map { it.first!! }

    private fun <A, B> List<Pair<A?, B?>>.takeSecondWhereFirstIsNull(): List<B> =
            this.filter { it.first == null }.map { it.second!! }
}
