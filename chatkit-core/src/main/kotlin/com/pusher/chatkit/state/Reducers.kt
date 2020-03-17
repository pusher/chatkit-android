package com.pusher.chatkit.state

import org.reduxkotlin.ReducerForActionType
import org.reduxkotlin.reducerForActionType

/**
 * Create a slice reducer for ChatState branch
 */
internal inline fun <reified TAction> chatStateReducer(
    crossinline reducer: ReducerForActionType<ChatState, TAction>
) = reducerForActionType<State, TAction> { state, action ->
    state.with(
        chatState = reducer(state.chatState, action)
    )
}
