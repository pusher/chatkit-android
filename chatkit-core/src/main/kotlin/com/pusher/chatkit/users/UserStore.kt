package com.pusher.chatkit.users

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class UserStore(private val userMap : ConcurrentMap<String, User> = ConcurrentHashMap()) {

    internal operator fun plusAssign(user: User) {
        userMap[user.id] = user
    }

    internal operator fun plusAssign(users: List<User>) {
        userMap += users.map { it.id to it }.toMap()
    }

    internal operator fun get(id: String): User? = userMap[id]

}
