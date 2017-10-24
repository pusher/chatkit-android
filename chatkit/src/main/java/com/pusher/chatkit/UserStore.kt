package com.pusher.chatkit

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class UserStore {
    private val members: ConcurrentMap<String, User>
    init {
        members = ConcurrentHashMap()
    }

    fun addOrMerge(user: User){
        if(members[user.id] != null){
            members[user.id]!!.updateWithPropertiesOfUser(user)
        }
        else{
            members.put(user.id, user)
        }
    }

    fun remove(userId: String) {
        members.remove(userId)
    }
}