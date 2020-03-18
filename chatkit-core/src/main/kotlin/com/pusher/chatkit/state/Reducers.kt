package com.pusher.chatkit.state

import org.reduxkotlin.ReducerForActionType
import org.reduxkotlin.reducerForActionType

/**
 * Create a slice reducer for ChatState branch
 */
internal inline fun <reified TAction> chatStateReducer(
    crossinline reducer: ReducerForActionType<ChatState, TAction>
) = stateReducer<TAction> { state, action ->
    state.with(
        chatState = reducer(state.chatState, action)
    )
}

/**
 * Create a reducer for the SubscriptionState branch
 */
internal inline fun <reified TAction> subscriptionStateReducer(
    crossinline reducer: ReducerForActionType<SubscriptionState, TAction>
) = stateReducer<TAction> { state, action ->
    state.with(
        subscriptionState = reducer(state.subscriptionState, action)
    )
}

/**
 * Create a reducer for the whole State
 */
private inline fun <reified TAction> stateReducer(
    crossinline reducer: ReducerForActionType<State, TAction>
) = reducerForActionType<State, TAction> { state, action ->
    reducer(state, action)
}
