package com.pusher.chatkit.users.state

import com.pusher.chatkit.state.CurrentUserReceived
import com.pusher.chatkit.state.chatStateReducer

internal val currentUserReceivedReducer =
    chatStateReducer<CurrentUserReceived> { chatState, action ->
        chatState.with(currentUser = action.currentUser)
    }
