package com.pusher.chatkit;

import elements.Error;

public interface RoomSubscriptionListeners {
    void onNewMessage(Message message);
    void userStartedTyping(User user);
    void userStoppedTyping(User user);
    void userJoined(User user);
    void userLeft(User user);
    void userCameOnline(User user);
    void userWentOffline(User user);
    void usersUpdated();
    void onError(Error error);
}
