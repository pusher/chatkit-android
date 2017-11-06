package com.pusher.chatkit.sample;

import android.app.Application;

import com.pusher.chatkit.ChatManager;
import com.pusher.chatkit.ChatkitTokenProvider;
import com.pusher.chatkit.CurrentUser;
import com.pusher.chatkit.CurrentUserListener;
import com.pusher.chatkit.Room;
import com.pusher.chatkit.User;
import com.pusher.chatkit.UserSubscriptionListenersAdapter;

import elements.Error;
import timber.log.Timber;

public class ChatApplication extends Application {

    private static final String INSTANCE_LOCATOR = "v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad";
    private static final String TOKEN_PROVIDER_ENDPOINT = "https://us1.pusherplatform.io/services/chatkit_token_provider/v1/c090a50e-3e0e-4d05-96b0-a967ee4717ad/token?instance_locator=v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad";
    private static final String USER_NAME = "zan";

    private ChatManager chatManager;
    private CurrentUser currentUser;
    private CurrentUserListener currentUserListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        ChatkitTokenProvider tokenProvider = new ChatkitTokenProvider(
                TOKEN_PROVIDER_ENDPOINT,
                USER_NAME
        );

        chatManager = new ChatManager.Builder()
                .instanceLocator(INSTANCE_LOCATOR)
                .context(getApplicationContext())
                .tokenProvider(tokenProvider)
                .build();

        chatManager.connect(
                new UserSubscriptionListenersAdapter(){
                    @Override
                    public void currentUserReceived(CurrentUser currentUser) {
                        ChatApplication.this.currentUser = currentUser;
                        if(currentUserListener != null){
                            currentUserListener.onCurrentUser(currentUser);
                            currentUserListener = null;
                        }
                    }

                    @Override
                    public void onError(Error error) {
                        super.onError(error);
                        Timber.d("Error %s", error.toString());
                    }

                    @Override
                    public void removedFromRoom(int roomId) {
                        super.removedFromRoom(roomId);
                        Timber.d("Removed from room: %d", roomId);
                    }

                    @Override
                    public void addedToRoom(Room room) {
                        super.addedToRoom(room);
                        Timber.d("Removed from room: %s", room);

                    }

                    @Override
                    public void roomDeleted(int roomId) {
                        super.roomDeleted(roomId);
                        Timber.d("Room deleted: %d", roomId);

                    }

                    @Override
                    public void roomUpdated(Room room) {
                        super.roomUpdated(room);
                        Timber.d("Room updated %s", room);

                    }

                    @Override
                    public void userCameOnline(User user) {
                        super.userCameOnline(user);
                        Timber.d("User came online%s", user);

                    }

                    @Override
                    public void userJoined(User user, Room room) {
                        super.userJoined(user, room);
                        Timber.d("User joined room: %s, %s ", user, room);

                    }

                    @Override
                    public void userLeft(User user, Room room) {
                        super.userLeft(user, room);
                        Timber.d("User left room: %s, %s ", user, room);

                    }

                    @Override
                    public void userWentOffline(User user) {
                        super.userWentOffline(user);
                        Timber.d("User went offline %s", user);

                    }
                });
    }

    public void getCurrentUser(CurrentUserListener listener){
        if(currentUser != null) {
            listener.onCurrentUser(currentUser);
            currentUserListener = null;
        }
        else{
            this.currentUserListener = listener;
        }
    }

    public ChatManager getChatManager(){
        return chatManager;
    }
}
