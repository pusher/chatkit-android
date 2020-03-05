package com.pusher.chatkit.users.state

import com.pusher.chatkit.state.CurrentUserReceived
import com.pusher.chatkit.state.State
import org.reduxkotlin.reducerForActionType

internal val currentUserReceivedReducer =
    reducerForActionType<State, CurrentUserReceived> { state, action ->
        state.with(action.currentUser, state.auxiliaryState.with(action))
    }
