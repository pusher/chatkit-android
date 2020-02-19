package com.pusher.chatkit.cursors.api

internal sealed class CursorSubscriptionEvent {
    data class OnCursorSet(val cursor: CursorApiType) : CursorSubscriptionEvent()
    data class InitialState(val cursors: List<CursorApiType>) : CursorSubscriptionEvent()
}

internal typealias CursorSubscriptionConsumer = (CursorSubscriptionEvent) -> Unit
