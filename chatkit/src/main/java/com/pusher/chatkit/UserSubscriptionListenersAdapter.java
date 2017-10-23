package com.pusher.chatkit;

import elements.Error;

/**
 * Adapterful of empty implementations.
 * */
public abstract class UserSubscriptionListenersAdapter implements UserSubscriptionListeners {

    @Override
    public void onError(Error error) {}

    @Override
    public void onCurrentUser(CurrentUser currentUser) {}

    @Override
    public void onRemovedFromRoom(Room room) {}

    @Override
    public void onUserJoined(int roomId, String userId) {}

    @Override
    public void onUserLeft(int roomId, String userId) {}

    @Override
    public void onRoomDeleted(int roomId) {}

    @Override
    public void onRoomUpdated(Room room) {}
}
