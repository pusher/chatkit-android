package com.pusher.chatkit

import android.arch.lifecycle.*
import java.util.concurrent.ConcurrentHashMap

class AndroidAppHookEmitter : AppHookEmitter {
    private val registeredListeners = ConcurrentHashMap<AppHookListener, LifecycleObserver>()

    override fun register(listener: AppHookListener) {
        val lifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart(source: LifecycleOwner) {
                listener.onAppOpened()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop(source: LifecycleOwner) {
                listener.onAppClosed()
            }
        }

        registeredListeners[listener] = lifecycleObserver

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    override fun unregister(listener: AppHookListener) {
        val lifecycleObserver = registeredListeners[listener]
        if (lifecycleObserver != null) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
            registeredListeners.remove(listener)
        }
    }
}
