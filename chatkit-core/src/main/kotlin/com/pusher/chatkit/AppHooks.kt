package com.pusher.chatkit

interface AppHookEmitter {
    fun register(listener: AppHookListener)
    fun unregister(listener: AppHookListener)
}

interface AppHookListener {
    fun onAppOpened()
    fun onAppClosed()
}
