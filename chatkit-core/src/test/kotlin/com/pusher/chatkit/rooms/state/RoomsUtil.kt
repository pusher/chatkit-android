package com.pusher.chatkit.rooms.state

object RoomsUtil {

    internal val roomOneId = "id1"
    internal val roomOne = JoinedRoomInternalType(
            roomOneId,
            "room1",
            false,
            1582283111,
            1582283111,
            null,
            null,
            null
    )
    internal val roomOneUpdated = JoinedRoomInternalType(
            roomOneId,
            "room1-Updated",
            false,
            1582283114,
            1582283114,
            null,
            null,
            null
    )

    internal val roomTwoId = "id2"
    internal val roomTwo = JoinedRoomInternalType(
            roomTwoId,
            "room2",
            false,
            1582283112,
            1582283112,
            null,
            null,
            null
    )

    internal val roomThreeId = "id3"
    internal val roomThree = JoinedRoomInternalType(
            roomThreeId,
            "room3",
            false,
            1582283113,
            1582283113,
            null,
            null,
            null
    )
}
