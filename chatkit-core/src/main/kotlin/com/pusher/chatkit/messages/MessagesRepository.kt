@file:Suppress("unused") // TODO: remove when no longer just a sketch (unused)

package com.pusher.chatkit.messages

import com.pusher.chatkit.PaginationState
import elements.Error

sealed class MessagesState {

    data class Initializing(val error: Error? = null) : MessagesState()

    data class Connected(
        val messages: List<Message>,
        val changeDescription: ChangeDescription?,
        val paginationState: PaginationState
    ) : MessagesState()

    data class Degraded(
        val messages: List<Message>,
        val changeDescription: ChangeDescription?,
        val paginationState: PaginationState,
        val error: Error
    ) : MessagesState()

    object Closed : MessagesState()

    sealed class ChangeDescription {

        data class OlderMessagesFetched(
            val fromPosition: Int,
            val toPosition: Int
        ) : ChangeDescription()

        data class NewMessageReceived(
            val position: Int
        ) : ChangeDescription()

        data class MessageUpdated(
            val position: Int,
            val previousValue: Message
        ) : ChangeDescription()

        data class MessageDeleted(
            val position: Int,
            val previousValue: Message
        ) : ChangeDescription()
    }
}

class MessagesRepository {

    val messages: List<Message> get() = TODO()

    fun observe(observer: (MessagesState) -> Unit) {
        // observe the store and translate delegating stuff to a mapper
        observer(MessagesState.Initializing())
    }

    fun close() {}
}
