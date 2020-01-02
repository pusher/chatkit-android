package com.pusher.chatkit.rooms

import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.RoomApiType
import com.pusher.chatkit.users.RoomMembershipApiType
import com.pusher.chatkit.users.UserInternalEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import java.util.*
import kotlin.collections.LinkedHashMap

internal class RoomStore {
    private val rooms: MutableMap<String, RoomApiType> = Collections.synchronizedMap(LinkedHashMap())
    private val readStates: MutableMap<String, ReadStateApiType> = Collections.synchronizedMap(LinkedHashMap())
    private val members: MutableMap<String, RoomMembershipApiType> = Collections.synchronizedMap(LinkedHashMap())

    operator fun get(id: String): Room? =
            rooms[id]?.let { Room(it, members[id], readStates[id]) }

    internal fun listAll(): List<Room> =
            rooms.keys.map { this[it]!! }

    internal fun clear() {
        rooms.clear()
        readStates.clear()
        members.clear()
    }

    internal fun initialiseContents(rooms: List<RoomApiType>, memberships: List<RoomMembershipApiType>, readStates: List<ReadStateApiType>) {
        clear()
        rooms.forEach { this.rooms[it.id] = it }
        memberships.forEach { this.members[it.roomId] = it }
        readStates.forEach { this.readStates[it.roomId] = it }
    }

    private fun remove(roomId: String) {
        this.rooms.remove(roomId)
        this.members.remove(roomId)
        this.readStates.remove(roomId)
    }

    fun applyUserSubscriptionEvent(
            event: UserSubscriptionEvent
    ): List<UserInternalEvent> =
            when (event) {
                is UserSubscriptionEvent.InitialState -> {
                    val addedRooms = event.rooms.map { it.id }.toSet() - this.rooms.keys.toSet()
                    val removedRooms = this.rooms.keys.toSet() - event.rooms.map { it.id }.toSet()

                    val changedRooms = this.rooms.values.mapNotNull { existing->
                        event.rooms.find { it.id == existing.id }?.to(existing)
                    }.filter { (new, existing) -> new != existing
                    }.map { it.first }

                    val changedReadStates = this.readStates.values.mapNotNull { existing ->
                        event.readStates.find { it.roomId == existing.roomId }?.to(existing)
                    }.filter { (new, existing) -> new != existing
                    }.map { it.first }

                    val changedMembers = this.members.values.mapNotNull { existing ->
                        event.memberships.find { it.roomId == existing.roomId }?.to(existing)
                    }.filterNot { (new, existing) ->
                        new.userIds.toSet() == existing.userIds.toSet()
                    }

                    this.initialiseContents(event.rooms, event.memberships, event.readStates)

                    addedRooms.map { UserInternalEvent.AddedToRoom(this[it]!!) } +
                    removedRooms.map { UserInternalEvent.RemovedFromRoom(it) } +
                    changedRooms.map { UserInternalEvent.RoomUpdated(this[it.id]!!) } +
                    changedReadStates.map { UserInternalEvent.RoomUpdated(this[it.roomId]!!) } +
                    changedMembers.flatMap { (new, existing) ->
                        (new.userIds - existing.userIds).map { UserInternalEvent.UserJoinedRoom(it, new.roomId) } +
                        (existing.userIds - new.userIds).map { UserInternalEvent.UserLeftRoom(it, new.roomId) }
                    }
                }
                is UserSubscriptionEvent.AddedToRoomEvent -> {
                    this.rooms[event.room.id] = event.room
                    this.members[event.memberships.roomId] = event.memberships
                    this.readStates[event.readState.roomId] = event.readState

                    listOf(UserInternalEvent.AddedToRoom(this[event.room.id]!!))
                }
                is UserSubscriptionEvent.RoomUpdatedEvent -> {
                    this.rooms[event.room.id] = event.room
                    listOf(UserInternalEvent.RoomUpdated(this[event.room.id]!!))
                }
                is UserSubscriptionEvent.ReadStateUpdatedEvent -> {
                    this.readStates[event.readState.roomId] = event.readState
                    listOf(
                            UserInternalEvent.RoomUpdated(this[event.readState.roomId]!!),
                            UserInternalEvent.NewCursor(event.readState.cursor!!) // can this be null?
                    )
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
                                RoomMembershipApiType(event.roomId, existingMembers.userIds + event.userId)
                    }
                    listOf(UserInternalEvent.UserJoinedRoom(event.userId, event.roomId))
                }
                is UserSubscriptionEvent.UserLeftRoomEvent -> {
                    this.members[event.roomId]?.let { existingMembers ->
                        this.members[event.roomId] =
                                RoomMembershipApiType(event.roomId, existingMembers.userIds - event.userId)
                    }
                    listOf(UserInternalEvent.UserLeftRoom(event.userId, event.roomId))
                }
                is UserSubscriptionEvent.ErrorOccurred ->
                    listOf(UserInternalEvent.ErrorOccurred(event.error))
            }
}
