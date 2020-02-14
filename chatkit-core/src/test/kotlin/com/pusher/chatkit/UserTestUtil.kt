package com.pusher.chatkit

import com.pusher.chatkit.users.User
import com.pusher.chatkit.util.dateFormat
import java.util.Date

fun simpleUser(id: String): User {
    val createdUpdatedAt = dateFormat.format(Date())
    return User(id, createdUpdatedAt, createdUpdatedAt, null, null)
}
