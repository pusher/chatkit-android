package com.pusher.chatkit.test

import com.pusher.chatkit.AppHookEmitter
import com.pusher.chatkit.AppHookListener

class NoAppHooks : AppHookEmitter {
    override fun register(listener: AppHookListener) {}
    override fun unregister(listener: AppHookListener) {}
}