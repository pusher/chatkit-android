package com.pusher.chatkit

import com.pusher.chatkit.rooms.Room

fun simpleRoom(id: String, name: String, isPrivate: Boolean = false, customData: CustomData? = null,
               unreadCount : Int? = null,
               // Gson puts null here but for the tests empty set should be fine too
               memberUserIds: Set<String> = emptySet()
) =
        Room(id, "ham", name, null, isPrivate, customData, unreadCount,
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                memberUserIds)

fun newEmptyJoinedRoom(id: String, name: String, creatorAndMemberId: String,
                       unreadCount: Int? = 0) =
        Room(id, creatorAndMemberId, name, null, false, null, unreadCount,
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                setOf(creatorAndMemberId))

fun newEmptyJoinedRoomWithNoUnreadCount(id: String, name: String, creatorAndMemberId: String) =
        newEmptyJoinedRoom(id, name, creatorAndMemberId, null)