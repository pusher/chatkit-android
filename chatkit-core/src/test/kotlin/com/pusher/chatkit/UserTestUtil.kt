package com.pusher.chatkit

import com.pusher.chatkit.users.api.UserApiType
import com.pusher.chatkit.util.dateFormat
import java.util.Date

internal fun simpleUser(id: String) : UserApiType {
    val createdUpdatedAt = dateFormat.format(Date())
    return UserApiType(id, createdUpdatedAt, createdUpdatedAt, null, null,
            null, false)
 }
