package com.pusher.chatkit

import com.pusher.platform.Instance

class CurrentUser(
        val id: String,
        val createdAt: String,
        val updatedAt: String,

        val name: String?,
        val avatarURL: String?,
        val customData: CustomData?,

        val rooms: MutableList<Room> = ArrayList<Room>(),
        val instance: Instance,
        val userStore: UserStore
) {
    val roomStore = RoomStore(instance = instance, rooms = rooms)

    init {
        TODO()
    }

    //Room membership related information
    fun createRoom(){
        TODO()
    }

    fun addUsers(){
        TODO()
    }

    fun removeUsers(){
        TODO()
    }


    /**
     * Update a room
     * */

    fun updateRoom(
            roomId: Int,
            name: String? = null,
            isPrivate: Boolean = false,
            onComplete: Any
    ){
        TODO()
    }

    /**
     * Delete a room
     * */
    fun deleteRoom(
            roomId: Int,
            onComplete: Any
    ){
        TODO()
    }

    /**
     * Join a room
     * */
    fun joinRoom(
            roomId: Int,
            onComplete: Any
    ){
        TODO()
    }

    /**
     * Leave a room
     * */
    fun leaveRoom(
            roomId: Int,
            onComplete: Any
    ){
        TODO()
    }

    //TODO: All the other shit - typealias, messages for room, etc...

}