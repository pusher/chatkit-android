package com.pusher.chatkit.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;

import com.pusher.chatkit.ChatManager;

public class MainActivity extends Activity {

    public static final String INSTANCE_ID = "";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        ChatManager chatManager = new ChatManager(
                INSTANCE_ID,
                getApplicationContext(),
                null,
                null
        );

//        chatManager.
    }
}
