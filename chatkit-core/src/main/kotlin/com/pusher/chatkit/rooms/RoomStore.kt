package com.pusher.chatkit.rooms

import com.pusher.chatkit.rooms.api.JoinRoomResponse
import com.pusher.chatkit.rooms.api.JoinedRoomApiType
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.users.UserInternalEvent
import com.pusher.chatkit.users.UserSubscriptionEvent

internal class RoomStore {
    private val rooms: MutableMap<String, JoinedRoomApiType> = HashMap()
    private val unreadCounts: MutableMap<String, Int> = HashMap()
    private val members: MutableMap<String, Set<String>> = HashMap()

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
            unreadCounts.clear()
            members.clear()
        }
    }

    internal fun initialiseContents(rooms: List<JoinedRoomApiType>,
                                    memberships: List<RoomMembershipApiType>,
                                    readStates: List<RoomReadStateApiType>) {

        synchronized(this) {
            clear()
            rooms.forEach { this.rooms[it.id] = it }
            memberships.forEach { this.members[it.roomId] = it.userIds.toSet() }
            readStates.forEach { this.unreadCounts[it.roomId] = it.unreadCount }
        }
    }

    /*
     * Only rooms we are a member of should be added to the RoomStore, so we
     * only accept Create/JoinRoomResponses as proof (CreateRoomResponse is
     * a typealias for JoinRoomResponse)
     */
    internal fun add(data: JoinRoomResponse) {
        synchronized(this) {
            rooms[data.room.id] = data.room
            members[data.membership.roomId] = data.membership.userIds.toSet()
        }
    }

    internal fun remove(roomId: String) {
        synchronized(this) {
            this.rooms.remove(roomId)
            this.members.remove(roomId)
            this.unreadCounts.remove(roomId)
        }
    }

    fun applyUserSubscriptionEvent(
            event: UserSubscriptionEvent
    ): List<UserInternalEvent> = synchronized(this) {
        when (event) {
            is UserSubscriptionEvent.InitialState -> {
                val addedRooms = event.rooms.map { it.id }.toSet() - this.rooms.keys.toSet()
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

                this.initialiseContents(event.rooms, event.memberships, event.readStates)

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
                this.rooms[event.room.id] = event.room
                this.members[event.membership.roomId] = event.membership.userIds.toSet()
                this.unreadCounts[event.readState.roomId] = event.readState.unreadCount

                listOf(UserInternalEvent.AddedToRoom(this[event.room.id]!!))
            }
            is UserSubscriptionEvent.RoomUpdatedEvent -> {
                this.rooms[event.room.id] = event.room
                listOf(UserInternalEvent.RoomUpdated(this[event.room.id]!!))
            }
            is UserSubscriptionEvent.ReadStateUpdatedEvent -> {
                this.unreadCounts[event.readState.roomId] = event.readState.unreadCount
                listOf(UserInternalEvent.RoomUpdated(this[event.readState.roomId]!!))
            }
            is UserSubscriptionEvent.RoomDeletedEvent -> {
                this.remove(event.roomId)
                listOf(UserInternalEvent.RoomDeleted(event.roomId))
            }
            is UserSubscriptionEvent.RemovedFromRoomEvent -> {
                this.remove(event.roomId)
                listOf(UserInternalEvent.RemovedFromRoom(event.roomId))
            }
            is UserSubscriptionEvent.UserJoinedRoomEvent -> {
                this.members[event.roomId]?.let { existingMembers ->
                    this.members[event.roomId] =
                            existingMembers + event.userId
                }
                listOf(UserInternalEvent.UserJoinedRoom(event.userId, event.roomId))
            }
            is UserSubscriptionEvent.UserLeftRoomEvent -> {
                this.members[event.roomId]?.let { existingMembers ->
                    this.members[event.roomId] =
                            existingMembers - event.userId
                }
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
