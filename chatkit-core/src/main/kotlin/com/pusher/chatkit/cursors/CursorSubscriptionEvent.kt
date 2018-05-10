package com.pusher.chatkit.cursors

import elements.Error

internal sealed class CursorSubscriptionEvent {
    data class OnCursorSet(val cursor: Cursor) : CursorSubscriptionEvent()
    data class OnError(val error: Error) : CursorSubscriptionEvent()
    object NoEvent : CursorSubscriptionEvent()
}
