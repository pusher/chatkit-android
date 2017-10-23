package com.pusher.chatkit;

public interface UserSubscriptionListener extends ErrorListener, RoomSubscriptionListeners {
    void currentUserReceived(CurrentUser currentUser);

    void addedToRoom(Room room);
    void removedFromRoom(Room room);
    void roomUpdated(Room room);
    void roomDeleted(int roomId);
}
