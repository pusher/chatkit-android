package com.pusher.chatkit.rooms.state

import assertk.Assert
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.prop

object JoinedRoomsStateTestUtil {

    private fun simpleRoom(id: String, name: String) =
            JoinedRoomInternalType(
                    id,
                    name,
                    false,
                    1582283111,
                    1582283111,
                    null,
                    null,
                    null
            )

    internal val roomOneId = "id1"
    internal val roomOne = simpleRoom(roomOneId, "roomOne")
    internal val roomOneUpdated = simpleRoom(roomOneId, "roomOne-updated")

    internal val roomTwoId = "id2"
    internal val roomTwo = simpleRoom(roomTwoId, "roomTwo")

    internal val roomThreeId = "id3"
    internal val roomThree = simpleRoom(roomThreeId, "roomThree")
}

internal fun Assert<JoinedRoomsState>.isEmpty() {
    prop(JoinedRoomsState::rooms).isEmpty()
    prop(JoinedRoomsState::unreadCounts).isEmpty()
}

internal fun Assert<JoinedRoomsState>.containsOnly(
    vararg rooms: Pair<String, JoinedRoomInternalType>
) {
    prop(JoinedRoomsState::rooms).containsOnly(*rooms)
}

internal fun Assert<JoinedRoomsState>.containsOnlyUnreadCounts(
    vararg rooms: Pair<String, Int>
) {
    prop(JoinedRoomsState::unreadCounts).containsOnly(*rooms)
}
