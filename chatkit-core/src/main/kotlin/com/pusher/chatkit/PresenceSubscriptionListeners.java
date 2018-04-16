package com.pusher.chatkit;

public interface PresenceSubscriptionListeners {
    void userCameOnline(User user);
    void userWentOffline(User user);
}
