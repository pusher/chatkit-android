package com.pusher.chatkit.state

import org.reduxkotlin.ReducerForActionType
import org.reduxkotlin.reducerForActionType

/**
 * Convenience method to create a reducer with operates only on the ChatState section of the State
 * hierarchy.
 */
internal inline fun <reified TAction> chatReducerForActionType(
    crossinline reducer: ReducerForActionType<ChatState, TAction>
) = reducerForActionType<State, TAction> { state, action ->
    state.with(
        chatState = reducer(state.chatState, action)
    )
}
