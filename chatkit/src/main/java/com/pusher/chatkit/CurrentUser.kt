package com.pusher.chatkit

import com.pusher.platform.Instance
import elements.Subscription
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class CurrentUser(
        val id: String,
        val createdAt: String,
        var updatedAt: String,

        var name: String?,
        var avatarURL: String?,
        var customData: CustomData?,

        val rooms: ConcurrentMap<Int, Room> = ConcurrentHashMap<Int, Room>(),
        val instance: Instance,
        val userStore: UserStore
) {

    fun updateWithPropertiesOf(newUser: CurrentUser){
        updatedAt = newUser.updatedAt
        name = newUser.name
        customData = newUser.customData
    }

    var presenceSubscription: Subscription? = null

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