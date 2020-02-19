package com.pusher.chatkit.cursors

import com.pusher.chatkit.cursors.api.CursorApiType
import elements.Error

internal sealed class CursorSubscriptionEvent {
    data class OnCursorSet(val cursor: CursorApiType) : CursorSubscriptionEvent()
    data class InitialState(val cursors: List<CursorApiType>) : CursorSubscriptionEvent()
}

internal typealias CursorSubscriptionConsumer = (CursorSubscriptionEvent) -> Unit
