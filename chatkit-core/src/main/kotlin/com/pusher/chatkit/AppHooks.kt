package com.pusher.chatkit

interface AppHooks {
    fun register(
            appOpened: () -> Unit,
            appClosed: () -> Unit
    )
}
