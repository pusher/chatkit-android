package com.pusher.chatkit;

import elements.Error;

/**
 * Adapterful of empty implementations.
 * */
public class UserSubscriptionListenersAdapter implements UserSubscriptionListener {

    @Override
    public void onNewMessage(Message message) {

    }

    @Override
    public void userStartedTyping(User user) {

    }

    @Override
    public void userStoppedTyping(User user) {

    }

    @Override
    public void userJoined(User user) {

    }

    @Override
    public void userLeft(User user) {

    }

    @Override
    public void userCameOnline(User user) {

    }

    @Override
    public void userWentOffline(User user) {

    }

    @Override
    public void usersUpdated() {

    }

    @Override
    public void onError(Error error) {}

    @Override
    public void currentUserReceived(CurrentUser currentUser) {}

    @Override
    public void addedToRoom(Room room) {

    }

    @Override
    public void removedFromRoom(Room room) {}

    @Override
    public void roomDeleted(int roomId) {}

    @Override
    public void roomUpdated(Room room) {}
}
