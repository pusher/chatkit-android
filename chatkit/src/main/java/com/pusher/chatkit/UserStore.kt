package com.pusher.chatkit

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class UserStore {
    private val members: ConcurrentMap<String, User>

    init {
        members = ConcurrentHashMap()
    }

    fun addOrMerge(user: User) {
        members[user.id].also { restored ->
            when (restored) {
                null -> members[user.id] = user
                else -> restored.updateWithPropertiesOfUser(user)
            }
        }
    }

    fun remove(userId: String) {
        members.remove(userId)
    }

    operator fun plusAssign(user: User) =
        addOrMerge(user)

    operator fun plusAssign(users: List<User>) =
        users.forEach(::plusAssign)

}
