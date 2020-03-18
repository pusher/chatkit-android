package com.pusher.chatkit.state

internal data class State(
    val chatState: ChatState,
    val subscriptionState: SubscriptionState
) {

    companion object {
        fun initial() = State(
            chatState = ChatState.initial(),
            subscriptionState = SubscriptionState.initial()
        )
    }

    fun with(chatState: ChatState) = copy(
        chatState = chatState
    )

    fun with(subscriptionState: SubscriptionState) = copy(
        subscriptionState = subscriptionState
    )
}
