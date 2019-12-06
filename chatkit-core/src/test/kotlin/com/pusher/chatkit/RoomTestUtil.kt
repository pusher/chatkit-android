package com.pusher.chatkit

import com.pusher.chatkit.rooms.Room

fun simpleRoom(id: String, name: String, isPrivate: Boolean, customData: CustomData?,
               unreadCount : Int? = null) =
        Room(id, "ham", name, null, isPrivate, customData, unreadCount,
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                setOf()) // Gson puts null here but for the tests empty list should be fine too