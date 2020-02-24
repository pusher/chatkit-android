package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState

internal data class LastChange<T>(val version: Int, val changedData: T)

@Suppress("CopyWithoutNamedArguments") // nextVersion (legible) passes as first param many times
internal data class AuxiliaryState(
    val version: Int,
    val joinedRoomsReceived: LastChange<JoinedRoomsState>?
) {

    companion object {
        fun initial() = AuxiliaryState(
            0,
            null
        )
    }

    fun with(joinedRoomsState: JoinedRoomsState) =
        copy(nextVersion, joinedRoomsReceived = LastChange(nextVersion, joinedRoomsState))

    private val nextVersion = version + 1
}
