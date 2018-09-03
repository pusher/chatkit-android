package com.pusher.chatkit.cursors

import com.pusher.chatkit.ChatEvent
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.InstanceType
import com.pusher.chatkit.subscription.ChatkitSubscription
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.SubscriptionListeners
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import elements.Subscription
import elements.SubscriptionEvent

internal class CursorSubscription(
    private val path: String,
    private val chatManager: ChatManager,
    private val cursorsStore: CursorsStore,
    private val consumeEvent: (CursorSubscriptionEvent) -> Unit
): ChatkitSubscription {
    private var active = false
    private val logger = chatManager.dependencies.logger
    private lateinit var subscription: Subscription

    override fun connect(): ChatkitSubscription{
        subscription = ResolvableSubscription(
            path = path,
            listeners = SubscriptionListeners<ChatEvent>(
                onOpen = { headers ->
                    logger.verbose("[Cursor] OnOpen $headers")
                    active = true
                },
                onEvent = { event ->
                    val cursorEvent = event.toCursorEvent()
                        .recover { CursorSubscriptionEvent.OnError(it) }
                    consumeEvent(cursorEvent)
                    when(cursorEvent) {
                        is CursorSubscriptionEvent.OnCursorSet ->  cursorsStore[cursorEvent.cursor.userId] += cursorEvent.cursor
                        is CursorSubscriptionEvent.InitialState -> cursorsStore += cursorEvent.cursors
                    }
                },
                onError = { error ->
                    logger.verbose("[Cursor] Subscription errored: $error")
                    consumeEvent(CursorSubscriptionEvent.OnError(error))
                },
                onSubscribe = { logger.verbose("[Cursor] Subscription established") },
                onRetrying = { logger.verbose("[Cursor] Subscription lost. Trying again.") },
                onEnd = { error -> logger.verbose("[Cursor] Subscription ended with: $error") }
            ),
            messageParser = { it.parseAs() },
            chatManager = chatManager,
            instanceType = InstanceType.CURSORS
        ).connect()

        return this
    }

    override fun unsubscribe() {
        active = false
        subscription.unsubscribe()
    }
}

private fun SubscriptionEvent<ChatEvent>.toCursorEvent(): Result<CursorSubscriptionEvent, Error> = when (body.eventName) {
    "new_cursor" -> body.data.parseAs<Cursor>().map(CursorSubscriptionEvent::OnCursorSet)
    "initial_state" -> body.data.parseAs<CursorSubscriptionEvent.InitialState>()
    else -> CursorSubscriptionEvent.NoEvent.asSuccess<CursorSubscriptionEvent, Error>()
}.map { it }