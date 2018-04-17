package com.pusher.chatkit;

import android.support.annotation.NonNull;

public interface CurrentUserListener {
    void onCurrentUser(@NonNull CurrentUser user);
}
