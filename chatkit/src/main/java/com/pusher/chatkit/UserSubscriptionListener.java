package com.pusher.chatkit;

public interface UserSubscriptionListener extends ErrorListener {
    void currentUserReceived(CurrentUser currentUser);

    void userStartedTyping(User user);
    void userStoppedTyping(User user);
    void userJoined(User user, Room room);
    void userLeft(User user, Room room);
    void userCameOnline(User user);
    void userWentOffline(User user);
    void usersUpdated();
    void addedToRoom(Room room);
    void removedFromRoom(int roomId);
    void roomUpdated(Room room);
    void roomDeleted(int roomId);
}
