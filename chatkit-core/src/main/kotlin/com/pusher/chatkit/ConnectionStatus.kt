package com.pusher.chatkit

import elements.Error

sealed class ConnectionStatus {
    object Connected : ConnectionStatus()
    data class Connecting(val error: Error?) : ConnectionStatus()
    object Disconnected : ConnectionStatus()
}
