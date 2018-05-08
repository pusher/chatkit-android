package com.pusher.chatkit;

interface RoomSubscriptionListeners : ErrorListener {
    fun onNewMessage(message: Message)
}
