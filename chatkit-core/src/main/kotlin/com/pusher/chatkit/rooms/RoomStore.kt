package com.pusher.chatkit.rooms

import com.pusher.chatkit.rooms.api.*
import com.pusher.chatkit.users.UserInternalEvent
import com.pusher.chatkit.users.UserSubscriptionEvent

internal class RoomStore {
    private val rooms: MutableMap<String, JoinedRoomApiType> = HashMap()
    private val members: MutableMap<String, Set<String>> = HashMap()
    private val unreadCounts: MutableMap<String, Int> = HashMap()

    private val mapper = JoinedRoomInternalMapper()

    operator fun get(id: String): Room? =
            synchronized(this) {
                rooms[id]?.let { mapper.toRoom(it, members[id]!!, unreadCounts[id]) }
            }

    internal fun toList(): List<Room> =
            synchronized(this) {
                rooms.keys.map { this[it]!! }
            }.sortedByDescending {
                it.lastMessageAt ?: it.createdAt
            }

    internal fun clear() {
        synchronized(this) {
            rooms.clear()
            members.clear()
            unreadCounts.clear()
        }
    }

    internal fun initialiseContents(rooms: List<JoinedRoomApiType>,
                                    memberships: List<RoomMembershipApiType>,
                                    readStates: List<RoomReadStateApiType>) {

        synchronized(this) {
            clear()
            this.rooms += rooms.map { it.id to it }
            members += memberships.map { it.roomId to it.userIds.toSet() }
            unreadCounts += readStates.map { it.roomId to it.unreadCount }
        }
    }

    internal fun add(data: CreateRoomResponse) {
        synchronized(this) {
            val roomId = data.room.id
            rooms[roomId] = data.room
            members[roomId] = data.membership.userIds.toSet()
            unreadCounts[roomId] = 0
        }
    }

    internal fun add(data: JoinRoomResponse) {
        synchronized(this) {
            val roomId = data.room.id
            rooms[roomId] = data.room
            members[roomId] = data.membership.userIds.toSet()
            // unread count will arrive with corresponding added_to_room user subscription event
        }
    }

    internal fun remove(roomId: String) {
        synchronized(this) {
            rooms.remove(roomId)
            members.remove(roomId)
            unreadCounts.remove(roomId)
        }
    }

    fun applyUserSubscriptionEvent(
            event: UserSubscriptionEvent
    ): List<UserInternalEvent> = synchronized(this) {
        when (event) {
            is UserSubscriptionEvent.InitialState -> {
                val addedRooms = event.rooms.map { it.id }.toSet() - rooms.keys.toSet()
                val removedRooms = this.rooms.keys.toSet() - event.rooms.map { it.id }.toSet()

                val changedRooms = this.rooms.values.mapNotNull { existing ->
                    event.rooms.find { it.id == existing.id }?.let { new ->
                        existing to new
                    }
                }.filter { (existing, new) ->
                    new != existing
                }.map { (existing, _) ->
                    existing.id
                }

                val changedReadStates = this.unreadCounts.mapNotNull { (roomId, existing) ->
                    event.readStates.find { it.roomId == roomId }?.let { newReadState ->
                        Triple(roomId, existing, newReadState.unreadCount)
                    }
                }.filter { (_, existing, new) ->
                    new != existing
                }.map { (roomId, _, _) ->
                    roomId
                }

                val changedMembers = this.members.mapNotNull { (roomId, existing) ->
                    event.memberships.find { it.roomId == roomId }?.let { newMembership ->
                        Triple(roomId, existing, newMembership.userIds.toSet())
                    }
                }.filter { (_, existing, new) ->
                    new != existing
                }

                initialiseContents(event.rooms, event.memberships, event.readStates)

                val addedEvents = addedRooms.map { UserInternalEvent.AddedToRoom(this[it]!!) }
                val removedEvents = removedRooms.map { UserInternalEvent.RemovedFromRoom(it) }
                val updatedEvents = (changedRooms + changedReadStates).toSet().map {
                    UserInternalEvent.RoomUpdated(this[it]!!)
                }
                val membershipEvents = changedMembers.flatMap { (roomId, existing, new) ->
                    (new - existing).map { UserInternalEvent.UserJoinedRoom(it, roomId) } +
                            (existing - new).map { UserInternalEvent.UserLeftRoom(it, roomId) }
                }

                addedEvents + removedEvents + updatedEvents + membershipEvents
            }
            is UserSubscriptionEvent.AddedToRoomEvent -> {
                val roomId = event.room.id
                rooms[roomId] = event.room
                members[roomId] = event.membership.userIds.toSet()
                unreadCounts[roomId] = event.readState.unreadCount

                listOf(UserInternalEvent.AddedToRoom(this[roomId]!!))
            }
            is UserSubscriptionEvent.RoomUpdatedEvent -> {
                rooms[event.room.id] = event.room
                listOf(UserInternalEvent.RoomUpdated(this[event.room.id]!!))
            }
            is UserSubscriptionEvent.ReadStateUpdatedEvent -> {
                unreadCounts[event.readState.roomId] = event.readState.unreadCount
                listOf(UserInternalEvent.RoomUpdated(this[event.readState.roomId]!!))
            }
            is UserSubscriptionEvent.RoomDeletedEvent -> {
                remove(event.roomId)
                listOf(UserInternalEvent.RoomDeleted(event.roomId))
            }
            is UserSubscriptionEvent.RemovedFromRoomEvent -> {
                remove(event.roomId)
                listOf(UserInternalEvent.RemovedFromRoom(event.roomId))
            }
            is UserSubscriptionEvent.UserJoinedRoomEvent -> {
                members[event.roomId] = members[event.roomId]!! + event.userId
                listOf(UserInternalEvent.UserJoinedRoom(event.userId, event.roomId))
            }
            is UserSubscriptionEvent.UserLeftRoomEvent -> {
                members[event.roomId] = members[event.roomId]!! - event.userId
                listOf(UserInternalEvent.UserLeftRoom(event.userId, event.roomId))
            }
            is UserSubscriptionEvent.ErrorOccurred ->
                listOf(UserInternalEvent.ErrorOccurred(event.error))
        }
    }
}

private class JoinedRoomInternalMapper {

    fun toRoom(
            room: JoinedRoomApiType,
            members: Set<String>,
            unreadCount: Int?
    ) = Room(
            room.id,
            room.createdById,
            room.name,
            room.pushNotificationTitleOverride,
            room.private,
            room.customData,
            unreadCount,
            room.lastMessageAt,
            room.createdAt,
            room.updatedAt,
            room.deletedAt,
            members
    )

}