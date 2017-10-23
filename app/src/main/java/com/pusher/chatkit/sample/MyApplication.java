package com.pusher.chatkit.sample;

import android.app.Application;

import com.pusher.chatkit.ChatManager;
import com.pusher.chatkit.ChatkitTokenProvider;
import com.pusher.chatkit.CurrentUser;

import timber.log.Timber;

public class MyApplication extends Application {

    private static final String INSTANCE_ID = "v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad";
    private static final String TOKEN_PROVIDER_ENDPOINT = "https://us1.pusherplatform.io/services/chatkit_token_provider/v1/c090a50e-3e0e-4d05-96b0-a967ee4717ad/token?instance_id=v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad";
    private static final String USER_NAME = "zan";

    private ChatManager chatManager;
    private CurrentUser currentUser;
    private ChatkitTokenProvider tokenProvider;



    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        tokenProvider = new ChatkitTokenProvider(
                TOKEN_PROVIDER_ENDPOINT,
                USER_NAME
        );

        chatManager = new ChatManager.Builder()
                .instanceId(INSTANCE_ID)
                .context(getApplicationContext())
                .tokenProvider(tokenProvider)
                .build();







    }

    CurrentUser getCurrentUser(){
        return null;
    }

    ChatManager getChatManager(){
        return null;
    }
}
