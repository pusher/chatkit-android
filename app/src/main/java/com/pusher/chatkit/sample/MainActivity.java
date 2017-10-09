package com.pusher.chatkit.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;

import com.pusher.chatkit.ChatManager;
import com.pusher.chatkit.CurrentUser;
import com.pusher.chatkit.CurrentUserListener;
import com.pusher.chatkit.ErrorListener;
import com.pusher.platform.logger.LogLevel;

import elements.Error;
import timber.log.Timber;

public class MainActivity extends Activity {

    public static final String INSTANCE_ID = "v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad";

    ChatManager chatManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        chatManager = new ChatManager(
                INSTANCE_ID,
                getApplicationContext(),
                null, //TODO: tokenProvider
                LogLevel.VERBOSE
        );

        chatManager.connect(
                new CurrentUserListener() {
                    @Override
                    public void onCurrentUser(CurrentUser user) {
                        Timber.d("onCurrentUsers %s", user.toString());
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onError(Error error) {
                        Timber.d("onError %s", error.toString());
                    }
                }
        );
    }
}
