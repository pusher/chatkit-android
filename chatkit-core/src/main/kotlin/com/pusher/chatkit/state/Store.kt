package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.joinedRoomReducer
import com.pusher.chatkit.rooms.state.joinedRoomsReceivedReducer
import com.pusher.chatkit.rooms.state.leftRoomReducer
import com.pusher.chatkit.rooms.state.roomDeletedReducer
import com.pusher.chatkit.rooms.state.roomUpdatedReducer
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.reduxkotlin.combineReducers
import org.reduxkotlin.createStore

internal val createStoreModule: () -> Module = {
    module {
        single(named(store::subscribe.name)) { store.subscribe }
        single { store.getState }

        single { Dispatcher(store) }
    }
}

internal typealias Store = org.reduxkotlin.Store<ChatkitState>

internal class Dispatcher(private val store: Store) : (Action) -> Unit {

    override fun invoke(action: Action) {
        store.dispatch(action)
    }
}

private val store = createStore(
    combineReducers(
        joinedRoomsReceivedReducer,
        joinedRoomReducer,
        roomUpdatedReducer,
        leftRoomReducer,
        roomDeletedReducer
    ),
    ChatkitState.initial()
)