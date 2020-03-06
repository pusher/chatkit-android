package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.Action
import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.State
import org.reduxkotlin.GetState

internal class JoinedRoomsStateDiffer(val stateGetter: GetState<State>) {

    fun stateExists() = stateGetter().joinedRoomsState != null

    fun toActions(
        newRooms: List<JoinedRoomInternalType>,
        newUnreadCounts: Map<String, Int>
    ): List<Action> {
        return joinedRoomActions(newRooms, newUnreadCounts) +
            roomUpdatedActions(newRooms) +
            leftRoomActions(newRooms)
    }

    private val currentRooms get() = stateGetter().joinedRoomsState!!.rooms

    private fun joinedRoomActions(
        newRooms: List<JoinedRoomInternalType>,
        newUnreadCounts: Map<String, Int>
    ): List<Action> {
        val actions = mutableListOf<Action>()

        val addedRoomKeys =
            newRooms.map { it.id }.toSet() - currentRooms.keys.toSet()

        for (addedKey in addedRoomKeys) {
            val room = newRooms.find { it.id == addedKey }
            val unreadCount = newUnreadCounts[addedKey]

            if (room != null &&
                unreadCount != null
            ) {
                actions.add(
                    JoinedRoom(
                        room = room,
                        unreadCount = unreadCount
                    )
                )
            }
        }
        return actions
    }

    private fun roomUpdatedActions(
        newRooms: List<JoinedRoomInternalType>
    ): List<Action> {
        val actions = mutableListOf<Action>()

        val changedRooms = currentRooms.values.mapNotNull { existing ->
            newRooms.find { it.id == existing.id }?.let { new ->
                existing to new
            }
        }.filter { (existing, new) ->
            new != existing
        }

        for (room in changedRooms) {
            actions.add(RoomUpdated(room = room.second))
        }

        return actions
    }

    private fun leftRoomActions(
        newRooms: List<JoinedRoomInternalType>
    ): List<Action> {
        val actions = mutableListOf<Action>()

        val removedRoomKeys =
            currentRooms.keys.toSet() - newRooms.map { it.id }.toSet()

        for (removedKey in removedRoomKeys) {
            actions.add(LeftRoom(roomId = removedKey))
        }

        return actions
    }
}
