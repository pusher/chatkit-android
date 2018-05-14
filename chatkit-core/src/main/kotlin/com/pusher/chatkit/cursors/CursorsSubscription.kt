package com.pusher.chatkit.cursors

import com.pusher.chatkit.*
import com.pusher.chatkit.network.parseAs
import com.pusher.platform.SubscriptionListeners
import elements.*

internal class CursorsSubscription private constructor(
    chatManager: ChatManager,
    path: String,
    private val consumeEvent: (CursorSubscriptionEvent) -> Unit
) : Subscription {

    companion object {
        fun forRoom(chatManager: ChatManager, roomId: Int, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
            CursorsSubscription(chatManager, "/cursors/0/rooms/$roomId", consumeEvent)
        fun forUser(chatManager: ChatManager, userId: String, consumeEvent: (CursorSubscriptionEvent) -> Unit) =
            CursorsSubscription(chatManager, "/cursors/0/users/$userId", consumeEvent)
    }

    private val subscription = chatManager.cursorsInstance.subscribeResuming(
        path = path,
        tokenProvider = chatManager.tokenProvider,
        listeners = SubscriptionListeners<ChatEvent>(
            onEvent = { it.toCursorEvent().let(consumeEvent) },
            onError = { consumeEvent(CursorSubscriptionEvent.OnError(it)) }
        ),
        messageParser = { it.parseAs() }
    )

    private fun SubscriptionEvent<ChatEvent>.toCursorEvent() = when(body.eventName) {
        "cursor_set" -> body.cursor.map<CursorSubscriptionEvent> { CursorSubscriptionEvent.OnCursorSet(it) }
            .recover { CursorSubscriptionEvent.OnError(it) }
        else -> CursorSubscriptionEvent.NoEvent
    }

    private val ChatEvent.cursor
        get() = data.parseAs<Cursor>()

    override fun unsubscribe() {
        subscription.unsubscribe()
    }

}

