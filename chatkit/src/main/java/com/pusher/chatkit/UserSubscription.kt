package com.pusher.chatkit

import com.pusher.platform.Instance
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent

class UserSubscription(
        val instance: Instance,
        path: String,
        val userStore: GlobalUserStore,
        val tokenProvider: TokenProvider,
        val tokenParams: ChatkitTokenParams?,
        val logger: Logger,
        val listeners: UserSubscriptionListeners
) {

    var subscription: Subscription? = null
    lateinit var headers: Headers

    init {
        subscription = instance.subscribeResuming(
                path = path,
                listeners = SubscriptionListeners(
                        onOpen = { headers ->
                            logger.warn("OnOpen $headers")
                            this.headers = headers
                        },
                        onEvent = { event ->
                            logger.warn("Event $event")
                            handleEvent(event)
                        },
                        onError = { error ->
                            logger.warn("Error $error")
                            listeners.errorListener.onError(error)
                        },
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

            currentUser = CurrentUser(
                    id = initialState.currentUser.id,
                    name = initialState.currentUser.name,
                    updatedAt = initialState.currentUser.updatedAt,
                    createdAt = initialState.currentUser.createdAt,
                    avatarURL = initialState.currentUser.avatarURL,
                    customData = initialState.currentUser.customData,

                    userStore = userStore,
                    rooms = initialState.rooms,
                    instance = instance,
                    tokenProvider = tokenProvider,
                    tokenParams = tokenParams
            )
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

        if(combinedRoomUserIds.size > 0){
            fetchDetailsForUsers(
                    userIds = combinedRoomUserIds,
                    onComplete = UsersListener { users ->
                        if(wasExistingCurrentUser){
                            updateExistingRooms(roomsForConnection)
                        }
                        listeners.currentUserListener.onCurrentUser(currentUser!!)
                    },
                    onError = ErrorListener { error ->
                        logger.error("Failed fetching user details $error")
                        listeners.errorListener.onError(error)
                    })
        }
        else {
            listeners.currentUserListener.onCurrentUser(currentUser!!)
        }
    }

    private fun fetchDetailsForUsers(
            userIds: Set<String>,
            onComplete: UsersListener,
            onError: ErrorListener
    ) {

        userStore.fetchUsersWithIds(
                userIds = userIds,
                onComplete = UsersListener { users ->

                    currentUser!!.rooms().forEach { room ->
                        room.memberUserIds.forEach { userId ->
                            val user = users.find { it.id == userId }
                            if(user != null){
                                room.userStore().addOrMerge(user)
                            }
                        }
                    }

                    onComplete.onUsers(users)
                },
                onFailure = onError
        )
    }

    private fun updateExistingRooms(roomsForConnection: MutableList<Room>) {
        val roomsUserIsNoLongerAMemberOf = currentUser!!.rooms().subtract(roomsForConnection)

        roomsUserIsNoLongerAMemberOf.forEach { room ->
            listeners.removedFromRoomListener.removedFromRoom(room)
        }




    }
}