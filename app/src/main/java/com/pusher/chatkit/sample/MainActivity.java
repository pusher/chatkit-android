package com.pusher.chatkit.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;

import com.pusher.chatkit.ChatManager;
import com.pusher.chatkit.ChatkitTokenProvider;
import com.pusher.chatkit.CurrentUser;
import com.pusher.chatkit.CurrentUserListener;
import com.pusher.chatkit.ErrorListener;
import com.pusher.platform.logger.LogLevel;

import java.util.Map;
import java.util.TreeMap;

import elements.Error;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class MainActivity extends Activity {

    public static final String INSTANCE_ID = "v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad";

    ChatManager chatManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Map<String, String> tokenParams = new TreeMap<>();
//        tokenParams.put("user_id", "zan");

        ChatkitTokenProvider tokenProvider = new ChatkitTokenProvider(
                "https://us1.pusherplatform.io/services/chatkit_token_provider/v1/c090a50e-3e0e-4d05-96b0-a967ee4717ad/token?instance_id=v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad",
                "zan",
                tokenParams,
                new OkHttpClient()
        );

        chatManager = new ChatManager(
                INSTANCE_ID,
                getApplicationContext(),
                tokenProvider,
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
