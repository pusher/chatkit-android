package com.pusher.chatkit

data class Room(
        val id: Int,
        val createdById: String,
        var name: String,
        var isPrivate: Boolean,
        val createdAt: String,
        var updatedAt: String,
        var deletedAt: String,
        var memberUserIds: MutableList<String>,
        private var userStore: UserStore?
){

    fun userStore(): UserStore {
        if(userStore == null) userStore = UserStore()
        return userStore!!
    }

    fun removeUser(userId: String){
        memberUserIds.remove(userId)
        userStore().remove(userId)
    }

    fun updateWithPropertiesOfRoom(updatedRoom: Room){
        name = updatedRoom.name
        isPrivate = updatedRoom.isPrivate
        updatedAt = updatedRoom.updatedAt
        deletedAt = updatedRoom.deletedAt
        memberUserIds = updatedRoom.memberUserIds
    }
}
