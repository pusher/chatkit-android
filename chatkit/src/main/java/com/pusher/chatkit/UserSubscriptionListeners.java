package com.pusher.chatkit;

public interface UserSubscriptionListeners extends ErrorListener {
    void onCurrentUser(CurrentUser currentUser);

    void onRemovedFromRoom(Room room);

    void onRoomUpdated(Room room);
    void onRoomDeleted(int roomId);
    void onUserJoined(int roomId, String userId);
    void onUserLeft(int roomId, String userId);

}
