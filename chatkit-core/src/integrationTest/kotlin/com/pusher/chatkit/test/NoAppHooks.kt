package com.pusher.chatkit.test

import com.pusher.chatkit.AppHooks

class NoAppHooks : AppHooks {
    override fun register(appOpened: () -> Unit, appClosed: () -> Unit) {
        // No op
    }
}