package com.pusher.chatkit

import com.pusher.chatkit.users.User
import com.pusher.chatkit.util.dateFormat
import java.util.*

fun simpleUser(id: String): User {
    val createdUpdatedAt = dateFormat.format(Date())
    return User(id, createdUpdatedAt, createdUpdatedAt, null, null, null)
}