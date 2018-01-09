package com.pusher.chatkit;

public interface RoomSubscriptionListeners extends ErrorListener{
    void onNewMessage(Message message);
    void onCursorSet(Cursor cursor);
}
