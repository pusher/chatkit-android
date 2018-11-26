package com.pusher.chatkit

import android.arch.lifecycle.*

class AndroidAppHooks : AppHooks {
    override fun register(appOpened: () -> Unit, appClosed: () -> Unit) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : LifecycleObserver {
                    @OnLifecycleEvent(Lifecycle.Event.ON_START)
                    fun onStart(source: LifecycleOwner) {
                        appOpened.invoke()
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                    fun onStop(source: LifecycleOwner) {
                        appClosed.invoke()
                    }
                }
        )
    }
}

