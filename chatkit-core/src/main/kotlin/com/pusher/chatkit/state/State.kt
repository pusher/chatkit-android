package com.pusher.chatkit.state

internal data class State(
    val chatState: ChatState
) {

    companion object {
        fun initial() = State(
            chatState = ChatState.initial()
        )
    }

    fun with(chatState: ChatState) = copy(
        chatState = chatState
    )
}
