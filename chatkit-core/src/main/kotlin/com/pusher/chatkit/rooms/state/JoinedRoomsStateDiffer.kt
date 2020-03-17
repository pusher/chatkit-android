package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.state.Action
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.ReconnectJoinedRoom
import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.State
import org.reduxkotlin.GetState

internal class JoinedRoomsStateDiffer(private val stateGetter: GetState<State>) {

    fun stateExists() = stateGetter().chatState.joinedRoomsState != null

    fun toActions(
        newRooms: List<JoinedRoomInternalType>,
        newUnreadCounts: Map<String, Int>
    ): List<Action> =
        joinedRoomActions(newRooms, newUnreadCounts) +
            roomUpdatedActions(newRooms) +
            leftRoomActions(newRooms)

    private val currentRooms get() = stateGetter().chatState.joinedRoomsState!!.rooms

    private fun joinedRoomActions(
        newRooms: List<JoinedRoomInternalType>,
        newUnreadCounts: Map<String, Int>
    ): List<ReconnectJoinedRoom> =
        newRooms.filter { !currentRooms.contains(it.id) }
            .map { ReconnectJoinedRoom(it, newUnreadCounts[it.id]) }.toList()

    private fun roomUpdatedActions(
        newRooms: List<JoinedRoomInternalType>
    ): List<RoomUpdated> =
        currentRooms.values.mapNotNull { existing ->
            newRooms.find { it.id == existing.id }?.let { new ->
                existing to new
            }
        }.filter { (existing, new) ->
            new != existing
        }.map { RoomUpdated(room = it.second) }

    private fun leftRoomActions(
        newRooms: List<JoinedRoomInternalType>
    ): List<LeftRoom> =
        (currentRooms.keys.toSet() - newRooms.map { it.id }.toSet())
            .map { LeftRoom(roomId = it) }
}
