package com.pusher.chatkit

import com.pusher.chatkit.users.RoomApiType

internal fun simpleRoom(id: String, name: String, isPrivate: Boolean = false, customData: CustomData? = null) =
        RoomApiType(id, "ham", name, null, isPrivate, customData,
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z",
                "2017-04-13T14:10:38Z"
               )
