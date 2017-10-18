package com.pusher.chatkit

import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Subscription
import elements.SubscriptionEvent

class UserSubscription(
        instance: Instance,
        path: String,
        val userStore: GlobalUserStore,
        tokenProvider: TokenProvider?,
        tokenParams: ChatkitTokenParams?,
        val logger: Logger,
        onCurrentUser: CurrentUserListener,
        onError: ErrorListener
) {

    var subscription: Subscription? = null

    init {
        subscription = instance.subscribeResuming(
                path = path,
                listeners = SubscriptionListeners(
                        onOpen = { headers ->
                            logger.warn("OnOpen $headers")
                        },
                        onEvent = { event ->

                            logger.warn("Event $event")

                            handleEvent(event) },
                        onError = { error ->
                            logger.warn("Error $error")
                            onError.onError(error) },
                        onSubscribe = {
                            logger.warn("onSubscribe")
                        },
                        onRetrying = {
                            logger.warn("onRetrying")
                        },
                        onEnd = {
                            error ->
                            logger.warn("onEnd $error")

                        }
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams
        )

        logger.warn("User subscription JEBOTELED")
    }

    fun handleEvent(event: SubscriptionEvent) {

        logger.warn("Handle event: $event")

        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)
        when(chatEvent.eventName){
            "initial_state" -> {
                val body = ChatManager.GSON.fromJson<InitialState>(chatEvent.data, InitialState::class.java)
                handleInitialState(body)
            }
            "added_to_room" -> {
                val body = ChatManager.GSON.fromJson<AddedToRoom>(chatEvent.data, AddedToRoom::class.java)
//                body.handle()
//                handleAddedToRoom(body)
            }
            "removed_from_room" -> {

            }
            "new_message" -> {

            }
            "room_updated" -> {

            }
            "room_deleted" -> {

            }
            "user_joined" -> {

            }
            "user_left" -> {

            }

            else -> { throw Error("Invalid event name: ${chatEvent.eventName}") }
        }
    }

    private var currentUser: CurrentUser? = null

    private fun handleInitialState(initialState: InitialState) {
        logger.verbose("Initial state received $initialState")

        var wasExistingCurrentUser = currentUser != null

        if(currentUser != null){
            currentUser?.updateWithPropertiesOf(initialState.currentUser)
        }
        else{
            currentUser = initialState.currentUser
        }

        currentUser?.presenceSubscription?.unsubscribe()
        currentUser?.presenceSubscription = null

        val combinedRoomUserIds = mutableSetOf<String>()
        val roomsForConnection = mutableListOf<Room>()

        initialState.rooms.forEach { room ->
            combinedRoomUserIds.addAll(room.memberUserIds)
            roomsForConnection.add(room)

            currentUser!!.roomStore.addOrMerge(room)
        }

        fetchDetailsForUsers(combinedRoomUserIds)

        if(wasExistingCurrentUser){

            reconcileExistingRoomStoreWithRoomsReceivedOnConnection(roomsForConnection)
        }





//        if currentUser = initialState.currentUser

    }

    private fun fetchDetailsForUsers(userIds: Set<String>) {

        userStore.fetchUsersWithIds(
                userIds = userIds,
                onComplete = UsersListener { users ->

                    currentUser!!.roomStore.rooms.values.forEach { room ->
                        room.memberUserIds.forEach { userId ->
                            val user = users.find { it.id == userId }
                            if(user != null){
                                room.userStore.addOrMerge(user)
                            }
                        }
                    }
                },
                onFailure = ErrorListener {
                    error ->

                }
        )


//        currentUser!!.roomStore!!.rooms.forEach { t, u ->  }

        TODO("Do this")
        //PCUserStore.swift // fetchInitialUserInformationForUserIds(_ userIds: Set<String>, currentUser: PCCurrentUser) {

//        userStore.fetchUsersWithIds()

    }

    private val listener: UserSubscriptionListener = UserSubscriptionListener()

    class UserSubscriptionListener {

        fun removedFromRoom(room: Room){
            TODO()
        }
    }

    private fun reconcileExistingRoomStoreWithRoomsReceivedOnConnection(roomsForConnection: MutableList<Room>) {
        val roomsUserIsNoLongerAMemberOf = currentUser!!.rooms.values.toSet().subtract(roomsForConnection)



        roomsUserIsNoLongerAMemberOf.forEach { room ->
            listener.removedFromRoom(room)
        }




    }
}