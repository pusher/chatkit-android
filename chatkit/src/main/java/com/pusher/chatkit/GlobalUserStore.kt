package com.pusher.chatkit

import java.util.concurrent.ConcurrentHashMap

class GlobalUserStore {

    private val userMap = ConcurrentHashMap<String, User>()

    internal operator fun plusAssign(user: User) {
        userMap[user.id] = user
    }

    internal operator fun plusAssign(users: List<User>) {
        userMap += users.map { it.id to it }.toMap()
    }

    internal operator fun get(id: String): User? = userMap[id]

}
