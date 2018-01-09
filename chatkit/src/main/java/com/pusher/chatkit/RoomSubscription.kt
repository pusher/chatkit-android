package com.pusher.chatkit

import android.os.Handler
import android.os.Looper
import com.pusher.platform.SubscriptionListeners
import elements.Error
import elements.Headers
import elements.SubscriptionEvent

class RoomSubscription(user: CurrentUser, val room: Room, val userStore: GlobalUserStore, val listeners: RoomSubscriptionListeners) {

    val mainThread: Handler = Handler(Looper.getMainLooper())

    val subscriptionListeners = SubscriptionListeners(
            onOpen = { handleOpen(it) },
            onEvent = { handleMessage(it) },
            onError = { handleError(it) }
    )

    fun handleOpen(headers: Headers){
        //TODO("Not handled currently.")
    }

    fun handleMessage(event: SubscriptionEvent){

        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)

        if(chatEvent.eventName == "new_message"){

            val message= ChatManager.GSON.fromJson<Message>(chatEvent.data, Message::class.java)

            message.room = room
            userStore.fetchUsersWithIds(
                    userIds = setOf(message.userId),
                    onComplete = UsersListener { users ->
                        if(users.isNotEmpty())
                            message.user = users[0]
                        mainThread.post { listeners.onNewMessage(message) }

                    },
                    onFailure = ErrorListener {
                        mainThread.post { listeners.onNewMessage(message) }
                    })
        }
        else {
            TODO("Some weird shit has happened. Event received is of the wrong type ${chatEvent.eventName}")
        }

    }

    fun handleCursor(event: SubscriptionEvent) {
        val chatEvent = ChatManager.GSON.fromJson<ChatEvent>(event.body, ChatEvent::class.java)
        if (chatEvent.eventName != "cursor_set") {
            TODO("Some weird shit has happened. Event received is of the wrong type ${chatEvent.eventName}")
        }
        val cursor = ChatManager.GSON.fromJson<Cursor>(chatEvent.data, Cursor::class.java)
        cursor.room = room
        userStore.fetchUsersWithIds(
                userIds = setOf(cursor.userId),
                onComplete = UsersListener { users ->
                    if (users.isNotEmpty()) {
                        cursor.user = users[0]
                    }
                    mainThread.post { listeners.onCursorSet(cursor) }
                },
                onFailure = ErrorListener {
                    mainThread.post { listeners.onCursorSet(cursor) }
                }
        )
    }

    fun handleError(error: Error){
        mainThread.post { listeners.onError(error) }
    }
}