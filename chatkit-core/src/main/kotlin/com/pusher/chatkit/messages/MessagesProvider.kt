package com.pusher.chatkit.messages

import com.pusher.chatkit.PaginationState
import elements.Error

sealed class MessagesState {

    data class Initializing(val error: Error?): MessagesState()

    data class Connected(
            val messages: List<Message>,
            val changeDescription: ChangeDescription?,
            val paginationState: PaginationState
    ): MessagesState()

    data class Degraded(
            val messages: List<Message>,
            val changeDescription: ChangeDescription?,
            val paginationState: PaginationState,
            val error: Error
    ): MessagesState()

    object Closed: MessagesState()

    sealed class ChangeDescription {

        data class OlderMessagesFetched(
                val fromPosition: Int,
                val toPosition: Int
        ): ChangeDescription()

        data class NewMessageReceived(
                val position: Int
        ): ChangeDescription()

        data class MessageUpdated(
                val position: Int,
                val previousValue: Message
        ): ChangeDescription()

        data class MessageDeleted(
                val position: Int,
                val previousValue: Message
        ): ChangeDescription()

    }

}

class MessagesProvider {

    val messages: List<Message> get() = TODO()

    fun observe(observer: (MessagesState) -> Unit) { }

    fun close() {}

}