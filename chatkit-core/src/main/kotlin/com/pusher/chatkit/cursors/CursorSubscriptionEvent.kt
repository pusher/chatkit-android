package com.pusher.chatkit.cursors

import elements.Error

sealed class CursorSubscriptionEvent {
    data class OnCursorSet(val cursor: Cursor) : CursorSubscriptionEvent()
    data class InitialState(val cursors: List<Cursor>) : CursorSubscriptionEvent()
    data class OnError(val error: Error) : CursorSubscriptionEvent()
    object NoEvent : CursorSubscriptionEvent()
}

typealias CursorSubscriptionConsumer = (CursorSubscriptionEvent) -> Unit
